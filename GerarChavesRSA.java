import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class GerarChavesRSA {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair par = gen.generateKeyPair();

        String privatePem = "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(par.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----\n";

        String publicPem = "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(par.getPublic().getEncoded())
            + "\n-----END PUBLIC KEY-----\n";

        String chavePrivada = Base64.getEncoder().encodeToString(privatePem.getBytes());
        String chavePublica = Base64.getEncoder().encodeToString(publicPem.getBytes());

        System.out.println("Cole isso no seu .env:\n");
        System.out.println("JWT_RSA_PRIVATE_KEY=" + chavePrivada);
        System.out.println("JWT_RSA_PUBLIC_KEY=" + chavePublica);
    }
}