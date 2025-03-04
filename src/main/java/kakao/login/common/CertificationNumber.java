package kakao.login.common;

public class CertificationNumber {

    public static String getCertificationNumber (){

        String certificationNumber = "";

        for(int count = 0; count<4; count++)
            certificationNumber += (int)(Math.random() * 10); //원래 0~1사이의 수가 오는데 0~9사이 숫자 받아올 수 있도록 소수를 정수로 하기위해 *10해줌.


        return certificationNumber;
    }
}
