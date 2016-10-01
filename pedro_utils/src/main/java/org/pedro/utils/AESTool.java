package org.pedro.utils;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 
 * @author pedro.m
 */
public class AESTool
{
  private static final String CIPHER_MODE = "AES/ECB/PKCS5Padding";
  private static final String AES         = "AES";
  private static final String SHA_1       = "SHA-1";
  private static final String UTF_8       = "UTF-8";

  private final SecretKeySpec secretKeySpec;

  /**
   * 
   * @param key
   * @throws GeneralSecurityException 
   * @throws UnsupportedEncodingException 
   */
  public AESTool(final String key) throws GeneralSecurityException, UnsupportedEncodingException
  {
    final byte[] byteKeys = key.getBytes(UTF_8);
    final MessageDigest sha = MessageDigest.getInstance(SHA_1);
    byte[] digest = sha.digest(byteKeys);
    byte[] digest16 = copy(digest, 16);
    secretKeySpec = new SecretKeySpec(digest16, AES);
  }

  /**
   * 
   * @param source
   * @param length
   * @return
   */
  private static byte[] copy(final byte[] source, final int length)
  {
    final byte[] dest = new byte[length];

    for (int i = 0; i < length; i++)
    {
      dest[i] = source[i];
    }

    return dest;
  }

  /**
   * 
   * @param toEncrypt
   * @return
   * @throws GeneralSecurityException 
   * @throws UnsupportedEncodingException 
   */
  public String encrypt(final String toEncrypt) throws GeneralSecurityException, UnsupportedEncodingException
  {
    final Cipher cipher = Cipher.getInstance(CIPHER_MODE);
    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

    return new String(Base64Tool.encode(cipher.doFinal(toEncrypt.getBytes(UTF_8))));
  }

  /**
   * 
   * @param toDecrypt
   * @return
   * @throws GeneralSecurityException
   * @throws UnsupportedEncodingException
   */
  public String decrypt(final String toDecrypt) throws GeneralSecurityException, UnsupportedEncodingException
  {
    final Cipher cipher = Cipher.getInstance(CIPHER_MODE);
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

    return new String(cipher.doFinal(Base64Tool.decode(toDecrypt)), UTF_8);
  }

  /**
   * 
   * @param args
   */
  /*
  public static void main(final String[] args)
  {
    try
    {
      final String privateKey = "ma_clef_privee_pour_data_mobibalises_net";
      final AESTool tool = new AESTool(privateKey);
      final Scanner scanner = new Scanner(System.in);

      System.out.print("Chaine : ");
      final String input = scanner.next();

      if (input.endsWith("="))
      {
        final String decrypted = tool.decrypt(input);
        System.out.println("decrypted : " + decrypted);
      }
      else
      {
        final String encrypted = tool.encrypt(input);
        System.out.println("encrypted : " + encrypted);
      }
    }
    catch (final Throwable th)
    {
      th.printStackTrace(System.err);
    }
  }
  */
}
