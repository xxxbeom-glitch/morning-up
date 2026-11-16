# 출시 빌드 R8 규칙.
# Room, Hilt, kotlinx.serialization은 라이브러리에 들어 있는 규칙(consumer rules)을 써요.
# 여기에는 우리 코드만의 예외를 적어요.

# 진단 기록과 울림 상태를 JSON으로 저장할 때 쓰는 클래스 이름을 지켜요.
-keepclassmembers @kotlinx.serialization.Serializable class com.lumian.morningup.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
