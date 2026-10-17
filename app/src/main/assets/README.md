# Android 개발자 인증 (ADI) 스니펫
# Google Play 배포가 아니라면 이 파일이 없어도 됩니다 (OneStore 제출에 영향 없음).
#
# Play 배포를 준비할 때:
#   1) https://developer.android.com/developer-verification 에서 인증 시작
#   2) 화면에 표시된 스니펫을 아래 경로에 그대로 저장
#        app/src/main/assets/adi-registration.properties
#   3) 이 파일이 포함된 APK 를 OneStore 에 등록
#   4) OneStore 가 자체 서명키로 재서명한 APK 를 Play Console 에 업로드
#
# 스니펫은 인증 재질량 등 민감한 정보이므로 저장소에 커밋하지 않습니다.
app/src/main/assets/adi-registration.properties