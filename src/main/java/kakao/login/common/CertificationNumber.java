package kakao.login.common;

public class CertificationNumber {

    // 인증 번호를 생성하는 메서드
    public static String getCertificationNumber (){

        // 인증 번호를 저장할 문자열 변수 초기화
        String certificationNumber = "";

        // 4자리 숫자 인증 번호를 생성하기 위한 반복문
        // count는 0부터 3까지 반복되므로 총 4번 반복됨
        for(int count = 0; count < 4; count++) {
            // Math.random()은 0.0 (포함) ~ 1.0 (미포함) 사이의 랜덤한 소수를 반환
            // 이를 10배 곱한 후 (0 ~ 9 사이의 랜덤 정수) 형변환하여 정수로 취급
            // 이 정수를 인증 번호에 추가
            certificationNumber += (int)(Math.random() * 10);
        }

        // 생성된 인증 번호 반환
        return certificationNumber;
    }
}
