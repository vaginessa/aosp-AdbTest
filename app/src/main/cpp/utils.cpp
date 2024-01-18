//
// Created by Rohit Verma on 15-01-2024.
//

#include "utils.h"

#include <string>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include <vector>

#include <openssl/bn.h>
#include <openssl/pem.h>
#include <openssl/rsa.h>
#include <openssl/x509v3.h>

#include "logging.h"
#include "crypto_utils.h"

namespace adb {
    namespace file {
        bool WriteStringToFd(std::string_view content, int fd) {
            const char *p = content.data();
            size_t left = content.size();
            while (left > 0) {
                ssize_t n = TEMP_FAILURE_RETRY(write(fd, p, left));
                if (n == -1) {
                    return false;
                }
                p += n;
                left -= n;
            }
            return true;
        }

        static bool CleanUpAfterFailedWrite(const std::string &path) {
            int saved_errno = errno;
            unlink(path.c_str());
            errno = saved_errno;
            return false;
        }

        bool WriteStringToFile(const std::string &content, const std::string &path,
                               bool follow_symlinks) {
            int flags = O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC | O_BINARY |
                        (follow_symlinks ? 0 : O_NOFOLLOW);
            int fd = TEMP_FAILURE_RETRY(open(path.c_str(), flags, 0666));
            if (fd == -1) {
                return false;
            }
            bool ret = WriteStringToFd(content, fd) || CleanUpAfterFailedWrite(path);
            close(fd);
            return ret;
        }
    } // namespace file

    namespace crypto {
        namespace {
            const char kBasicConstraints[] = "critical,CA:TRUE";
            const char kKeyUsage[] = "critical,keyCertSign,cRLSign,digitalSignature";
            const char kSubjectKeyIdentifier[] = "hash";
            constexpr int kCertLifetimeSeconds = 10 * 365 * 24 * 60 * 60;

            bool add_ext(X509 *cert, int nid, const char *value) {
                size_t len = strlen(value) + 1;
                std::vector<char> mutableValue(value, value + len);
                X509V3_CTX context;

                X509V3_set_ctx_nodb(&context);

                X509V3_set_ctx(&context, cert, cert, nullptr, nullptr, 0);
                X509_EXTENSION *ex = X509V3_EXT_nconf_nid(nullptr, &context, nid,
                                                          mutableValue.data());
                if (!ex) {
                    return false;
                }

                X509_add_ext(cert, ex, -1);
                X509_EXTENSION_free(ex);
                return true;
            }
        }  // namespace

        bssl::UniquePtr<X509> GenerateX509Certificate(EVP_PKEY *pkey) {
            bssl::UniquePtr<X509> x509(X509_new());
            if (!x509) {
                LOGE("Unable to allocate x509 container");
                return nullptr;
            }
            X509_set_version(x509.get(), 2);

            ASN1_INTEGER_set(X509_get_serialNumber(x509.get()), 1);
            X509_gmtime_adj(X509_get_notBefore(x509.get()), 0);
            X509_gmtime_adj(X509_get_notAfter(x509.get()), kCertLifetimeSeconds);

            if (!X509_set_pubkey(x509.get(), pkey)) {
                LOGE("Unable to set x509 public key");
                return nullptr;
            }

            X509_NAME *name = X509_get_subject_name(x509.get());
            if (!name) {
                LOGE("Unable to get x509 subject name");
                return nullptr;
            }
            X509_NAME_add_entry_by_txt(name, "C", MBSTRING_ASC,
                                       reinterpret_cast<const unsigned char *>("US"), -1, -1, 0);
            X509_NAME_add_entry_by_txt(name, "O", MBSTRING_ASC,
                                       reinterpret_cast<const unsigned char *>("Android"), -1, -1,
                                       0);
            X509_NAME_add_entry_by_txt(name, "CN", MBSTRING_ASC,
                                       reinterpret_cast<const unsigned char *>("Adb"), -1, -1, 0);
            if (!X509_set_issuer_name(x509.get(), name)) {
                LOGE("Unable to set x509 issuer name");
                return nullptr;
            }

            add_ext(x509.get(), NID_basic_constraints, kBasicConstraints);
            add_ext(x509.get(), NID_key_usage, kKeyUsage);
            add_ext(x509.get(), NID_subject_key_identifier, kSubjectKeyIdentifier);

            int bytes = X509_sign(x509.get(), pkey, EVP_sha256());
            if (bytes <= 0) {
                LOGE("Unable to sign x509 certificate");
                return nullptr;
            }
            return x509;
        }

        std::string X509ToPEMString(X509 *x509) {
            bssl::UniquePtr<BIO> bio(BIO_new(BIO_s_mem()));
            int rc = PEM_write_bio_X509(bio.get(), x509);
            if (rc != 1) {
                LOGE("PEM_write_bio_X509 failed");
                return "";
            }

            BUF_MEM *mem = nullptr;
            BIO_get_mem_ptr(bio.get(), &mem);
            if (!mem || !mem->data || !mem->length) {
                LOGE("BIO_get_mem_ptr failed");
                return "";
            }
            return std::string(mem->data, mem->length);
        }

        std::string ToPEMString(EVP_PKEY *private_key) {
            bssl::UniquePtr<BIO> bio(BIO_new(BIO_s_mem()));
            int rc = PEM_write_bio_PKCS8PrivateKey(bio.get(), private_key, nullptr, nullptr, 0,
                                                   nullptr, nullptr);
            if (rc != 1) {
                LOGE("PEM_write_bio_PKCS8PrivateKey failed");
                return "";
            }

            BUF_MEM *mem = nullptr;
            BIO_get_mem_ptr(bio.get(), &mem);
            if (!mem || !mem->data || !mem->length) {
                LOGE("BIO_get_mem_ptr failed");
                return "";
            }
            return std::string(mem->data, mem->length);
        }
    } // namespace crypto
} // namespace adb