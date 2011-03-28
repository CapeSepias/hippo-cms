/*
 *  Copyright 2011 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.sso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

/**
 * Symmetric cipher that encrypts and decrypts jcr {@link Credentials}.
 * It's key is generated dynamically, being unique for the lifetime of the class.
 */
public final class CredentialCipher {

    private final static CredentialCipher instance = new CredentialCipher();

    public static CredentialCipher getInstance() {
        return instance;
    }

    private final SecretKeySpec secret;

    private CredentialCipher() {
        KeyGenerator kgen = null;
        try {
            kgen = KeyGenerator.getInstance("AES");
            kgen.init(128);
            secret = new SecretKeySpec(kgen.generateKey().getEncoded(), "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Encryption method AES could not be found", e);
        }

    }

    public byte[] encrypt(SimpleCredentials credentials) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secret);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(credentials);

            return cipher.doFinal(baos.toByteArray());
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        }
    }

    public Credentials decrypt(byte[] bytes) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secret);

            byte[] decrypted = cipher.doFinal(bytes);

            ByteArrayInputStream baos = new ByteArrayInputStream(decrypted);
            ObjectInputStream oos = new ObjectInputStream(baos);
            return (Credentials) oos.readObject();
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (IOException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not decrypt credentials", e);
        }
    }
}
