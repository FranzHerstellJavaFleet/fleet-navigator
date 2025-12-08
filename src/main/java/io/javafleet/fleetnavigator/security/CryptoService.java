package io.javafleet.fleetnavigator.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

/**
 * Cryptographic service for Mate authentication and encryption.
 * Uses Ed25519 for signing/verification and X25519 for key exchange.
 * AES-256-GCM for message encryption.
 */
@Slf4j
@Service
public class CryptoService {

    private static final String KEYS_FILE = "navigator_keys.json";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${fleet.data-dir:#{systemProperties['user.home'] + '/.fleet-navigator'}}")
    private String dataDir;

    private KeyPair signKeyPair;      // Ed25519 for signing
    private KeyPair exchangeKeyPair;  // X25519 for key exchange

    @PostConstruct
    public void init() {
        try {
            loadOrGenerateKeys();
            log.info("Crypto service initialized with Navigator public key: {}",
                    getPublicKeyBase64().substring(0, 20) + "...");
        } catch (Exception e) {
            log.error("Failed to initialize crypto service", e);
            throw new RuntimeException("Crypto initialization failed", e);
        }
    }

    /**
     * Load existing keys or generate new ones
     */
    private void loadOrGenerateKeys() throws Exception {
        Path keysPath = Path.of(dataDir, KEYS_FILE);

        if (Files.exists(keysPath)) {
            loadKeys(keysPath);
            log.info("Loaded existing Navigator keys from {}", keysPath);
        } else {
            generateNewKeys();
            saveKeys(keysPath);
            log.info("Generated new Navigator keys, saved to {}", keysPath);
        }
    }

    /**
     * Generate new Ed25519 and X25519 key pairs
     */
    private void generateNewKeys() throws NoSuchAlgorithmException {
        // Ed25519 for signing
        KeyPairGenerator signGen = KeyPairGenerator.getInstance("Ed25519");
        signKeyPair = signGen.generateKeyPair();

        // X25519 for key exchange
        KeyPairGenerator exchangeGen = KeyPairGenerator.getInstance("X25519");
        exchangeKeyPair = exchangeGen.generateKeyPair();

        log.debug("Generated new Ed25519 and X25519 key pairs");
    }

    /**
     * Save keys to JSON file
     */
    private void saveKeys(Path keysPath) throws IOException {
        Files.createDirectories(keysPath.getParent());

        // Simple JSON format
        String json = String.format("""
            {
                "signPrivateKey": "%s",
                "signPublicKey": "%s",
                "exchangePrivateKey": "%s",
                "exchangePublicKey": "%s"
            }
            """,
                Base64.getEncoder().encodeToString(signKeyPair.getPrivate().getEncoded()),
                Base64.getEncoder().encodeToString(signKeyPair.getPublic().getEncoded()),
                Base64.getEncoder().encodeToString(exchangeKeyPair.getPrivate().getEncoded()),
                Base64.getEncoder().encodeToString(exchangeKeyPair.getPublic().getEncoded())
        );

        Files.writeString(keysPath, json);
        // Secure the file permissions (Unix only)
        try {
            keysPath.toFile().setReadable(false, false);
            keysPath.toFile().setReadable(true, true);
            keysPath.toFile().setWritable(false, false);
            keysPath.toFile().setWritable(true, true);
        } catch (Exception e) {
            log.warn("Could not set file permissions on keys file");
        }
    }

    /**
     * Load keys from JSON file
     */
    private void loadKeys(Path keysPath) throws Exception {
        String json = Files.readString(keysPath);

        // Simple JSON parsing
        String signPrivB64 = extractJsonValue(json, "signPrivateKey");
        String signPubB64 = extractJsonValue(json, "signPublicKey");
        String exchPrivB64 = extractJsonValue(json, "exchangePrivateKey");
        String exchPubB64 = extractJsonValue(json, "exchangePublicKey");

        // Decode and reconstruct keys
        signKeyPair = reconstructKeyPair(signPrivB64, signPubB64, "Ed25519");
        exchangeKeyPair = reconstructKeyPair(exchPrivB64, exchPubB64, "X25519");
    }

    private String extractJsonValue(String json, String key) {
        int start = json.indexOf("\"" + key + "\"");
        if (start < 0) throw new IllegalArgumentException("Key not found: " + key);
        start = json.indexOf(":", start) + 1;
        start = json.indexOf("\"", start) + 1;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private KeyPair reconstructKeyPair(String privB64, String pubB64, String algorithm) throws Exception {
        byte[] privBytes = Base64.getDecoder().decode(privB64);
        byte[] pubBytes = Base64.getDecoder().decode(pubB64);

        KeyFactory kf = KeyFactory.getInstance(algorithm);
        PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        PublicKey pubKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));

        return new KeyPair(pubKey, privKey);
    }

    // ========== Public Key Access ==========

    // X.509 header lengths (OID + wrapper)
    private static final int ED25519_X509_HEADER_LENGTH = 12;  // 30 2a 30 05 06 03 2b 65 70 03 21 00
    private static final int X25519_X509_HEADER_LENGTH = 12;   // 30 2a 30 05 06 03 2b 65 6e 03 21 00

    /**
     * Get Navigator's Ed25519 public key as Base64 (raw 32 bytes for Go compatibility)
     */
    public String getPublicKeyBase64() {
        byte[] encoded = signKeyPair.getPublic().getEncoded();
        // Extract raw 32-byte key from X.509 format
        byte[] raw = new byte[32];
        System.arraycopy(encoded, ED25519_X509_HEADER_LENGTH, raw, 0, 32);
        return Base64.getEncoder().encodeToString(raw);
    }

    /**
     * Get Navigator's X25519 public key as Base64 (raw 32 bytes for Go compatibility)
     */
    public String getExchangePublicKeyBase64() {
        byte[] encoded = exchangeKeyPair.getPublic().getEncoded();
        // Extract raw 32-byte key from X.509 format
        byte[] raw = new byte[32];
        System.arraycopy(encoded, X25519_X509_HEADER_LENGTH, raw, 0, 32);
        return Base64.getEncoder().encodeToString(raw);
    }

    // ========== Signing ==========

    /**
     * Sign a message with Navigator's Ed25519 private key
     */
    public String sign(String message) throws GeneralSecurityException {
        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(signKeyPair.getPrivate());
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    // X.509 SubjectPublicKeyInfo header for Ed25519 (OID 1.3.101.112)
    private static final byte[] ED25519_X509_HEADER = new byte[] {
        0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
    };

    /**
     * Verify a signature from a Mate.
     * Accepts both raw 32-byte keys (from Go) and X.509 encoded keys (from Java).
     */
    public boolean verify(String message, String signatureB64, String publicKeyB64) {
        try {
            byte[] pubBytes = Base64.getDecoder().decode(publicKeyB64);

            // If it's a raw 32-byte key, wrap it with X.509 header
            if (pubBytes.length == 32) {
                log.debug("Converting raw Ed25519 key to X.509 format for verification");
                byte[] x509Key = new byte[ED25519_X509_HEADER.length + pubBytes.length];
                System.arraycopy(ED25519_X509_HEADER, 0, x509Key, 0, ED25519_X509_HEADER.length);
                System.arraycopy(pubBytes, 0, x509Key, ED25519_X509_HEADER.length, pubBytes.length);
                pubBytes = x509Key;
            }

            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            PublicKey pubKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));

            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(pubKey);
            sig.update(message.getBytes(StandardCharsets.UTF_8));

            byte[] signature = Base64.getDecoder().decode(signatureB64);
            return sig.verify(signature);
        } catch (Exception e) {
            log.warn("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    // ========== Key Exchange (X25519) ==========

    // X.509 SubjectPublicKeyInfo header for X25519 (OID 1.3.101.110)
    private static final byte[] X25519_X509_HEADER = new byte[] {
        0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00
    };

    /**
     * Derive a shared secret using X25519 key exchange.
     * Accepts both raw 32-byte keys (from Go) and X.509 encoded keys (from Java).
     */
    public byte[] deriveSharedSecret(String peerPublicKeyB64) throws GeneralSecurityException {
        byte[] pubBytes = Base64.getDecoder().decode(peerPublicKeyB64);

        // If it's a raw 32-byte key, wrap it with X.509 header
        if (pubBytes.length == 32) {
            log.debug("Converting raw X25519 key to X.509 format");
            byte[] x509Key = new byte[X25519_X509_HEADER.length + pubBytes.length];
            System.arraycopy(X25519_X509_HEADER, 0, x509Key, 0, X25519_X509_HEADER.length);
            System.arraycopy(pubBytes, 0, x509Key, X25519_X509_HEADER.length, pubBytes.length);
            pubBytes = x509Key;
        }

        KeyFactory kf = KeyFactory.getInstance("X25519");
        PublicKey peerKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));

        KeyAgreement ka = KeyAgreement.getInstance("X25519");
        ka.init(exchangeKeyPair.getPrivate());
        ka.doPhase(peerKey, true);

        // Hash the shared secret to get a proper AES key
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return sha256.digest(ka.generateSecret());
    }

    // ========== AES-256-GCM Encryption ==========

    /**
     * Encrypt a message using AES-256-GCM with the derived shared secret
     */
    public String encrypt(String plaintext, byte[] sharedSecret) throws GeneralSecurityException {
        SecretKey key = new SecretKeySpec(sharedSecret, "AES");

        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Combine IV + ciphertext
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt a message using AES-256-GCM with the derived shared secret
     */
    public String decrypt(String encryptedB64, byte[] sharedSecret) throws GeneralSecurityException {
        SecretKey key = new SecretKeySpec(sharedSecret, "AES");

        byte[] combined = Base64.getDecoder().decode(encryptedB64);

        // Extract IV and ciphertext
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    // ========== Pairing Code Generation ==========

    /**
     * Generate a 6-digit pairing code from the public keys
     */
    public String generatePairingCode(String matePublicKeyB64) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(matePublicKeyB64.getBytes(StandardCharsets.UTF_8));
            sha256.update(getPublicKeyBase64().getBytes(StandardCharsets.UTF_8));

            byte[] hash = sha256.digest();

            // Take first 3 bytes and convert to 6-digit number
            int code = ((hash[0] & 0xFF) << 16) | ((hash[1] & 0xFF) << 8) | (hash[2] & 0xFF);
            code = code % 1000000; // Ensure 6 digits

            return String.format("%06d", code);
        } catch (Exception e) {
            log.error("Failed to generate pairing code", e);
            return "000000";
        }
    }

    // ========== Challenge Generation ==========

    /**
     * Generate a random nonce for authentication challenges
     */
    public String generateNonce() {
        byte[] nonce = new byte[32];
        try {
            SecureRandom.getInstanceStrong().nextBytes(nonce);
        } catch (NoSuchAlgorithmException e) {
            new SecureRandom().nextBytes(nonce);
        }
        return Base64.getEncoder().encodeToString(nonce);
    }

    // ========== Fingerprint Generation ==========

    /**
     * Generate a short fingerprint from a public key for visual verification.
     * Format: xxxx:xxxx:xxxx (12 hex chars with colons)
     */
    public String generateFingerprint(String publicKeyB64) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(Base64.getDecoder().decode(publicKeyB64));

            // Take first 6 bytes (48 bits) and format as xxxx:xxxx:xxxx
            StringBuilder fp = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                fp.append(String.format("%02x", hash[i] & 0xFF));
                if (i == 1 || i == 3) {
                    fp.append(":");
                }
            }
            return fp.toString();
        } catch (Exception e) {
            log.error("Failed to generate fingerprint", e);
            return "0000:0000:0000";
        }
    }
}
