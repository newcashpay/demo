package com.jxchain.newpay.web.sandbox.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * 
 * @author wangyan
 *
 */
public class ECDSA {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	public static KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
		ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256k1");
        keyPairGenerator.initialize(ecGenParameterSpec, new SecureRandom());
		return keyPairGenerator.generateKeyPair();
	}

	public static byte[] sign(String privateKey, String content)
			throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, NoSuchProviderException {
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(HexUtils.fromHex(privateKey));
		KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
		PrivateKey priKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
		Signature signature = Signature.getInstance("SHA256withECDSA");
		signature.initSign(priKey);
		signature.update(content.getBytes());
		return signature.sign();
	}

	public static boolean verify(String publicKey, String content, String sig)
			throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, NoSuchProviderException {
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(HexUtils.fromHex(publicKey));
		KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
		PublicKey pubKey = keyFactory.generatePublic(x509EncodedKeySpec);
		Signature signature = Signature.getInstance("SHA256withECDSA");
		signature.initVerify(pubKey);
		signature.update(content.getBytes());
		return signature.verify(HexUtils.fromHex(sig));
	}

}
