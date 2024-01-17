//
// Created by Rohit Verma on 15-01-2024.
//

#ifndef ADB_AUTH_H
#define ADB_AUTH_H

#include <string>

#include <openssl/evp.h>

namespace adb {
    namespace auth {
        bool GenerateKey(const std::string &file);

        std::string GetPublicKey(std::string &file);

        bssl::UniquePtr<EVP_PKEY> GetPrivateKey(std::string &file);

        std::string SignToken(std::string &file, const char *token, size_t token_size);
    } // namespace auth
} // namespace adb

#endif // ADB_AUTH_H
