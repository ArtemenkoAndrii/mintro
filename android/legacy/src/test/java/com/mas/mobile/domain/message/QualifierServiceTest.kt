package com.mas.mobile.domain.message

import com.mas.mobile.DummyTaskService
import com.mas.mobile.domain.message.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QualifierServiceTest {
    private val mockMessageRuleRepository = mockk<QualifierRepository>(relaxed = true)

    private val testInstance = QualifierService(DummyTaskService, mockMessageRuleRepository)

    @BeforeAll
    fun setUp() {
        every { mockMessageRuleRepository.getQualifiers(Qualifier.Type.SKIP) }
            .returns(listOf(
                Qualifier(QualifierId(1), Qualifier.Type.SKIP,"canceled"),
                Qualifier(QualifierId(2), Qualifier.Type.SKIP, "verification code")
            ))
        every { mockMessageRuleRepository.getQualifiers(Qualifier.Type.CATCH) }
            .returns(listOf(
                Qualifier(QualifierId(3), Qualifier.Type.CATCH,"paid"),
                Qualifier(QualifierId(4), Qualifier.Type.CATCH,"payment of")
            ))
        every { mockMessageRuleRepository.getQualifiers(Qualifier.Type.BLACKLIST) }
            .returns(listOf(
                Qualifier(QualifierId(5), Qualifier.Type.CATCH,"revolut"),
                Qualifier(QualifierId(6), Qualifier.Type.CATCH,"google wallet")
            ))
    }

    @Test
    fun `Should recommend by word`() = assertTrue(
        testInstance.isRecommended("BBVA", "payment of 4,10 EUR in aliexpress with your card ending in 8850 accepted.")
    )

    @Test
    fun `Should recommend by phrase ignore case`() = assertTrue(
        testInstance.isRecommended("BbVa", "PaYmEnT of 4,10 EUR in aliexpress with your card ending in 8850 accepted.")
    )

    @Test
    fun `Should not recommend by skip word`() = assertFalse(
        testInstance.isRecommended("BBVA", "Payment of 4,10 EUR was canceled.")
    )

    @Test
    fun `Should not recommend by skip phrase ignore case`() = assertFalse(
        testInstance.isRecommended("BBVA", "Your verification CoDe is 8429. Please enter this code within the next 10 minutes to complete your verification process. Do not share this code with anyone.")
    )

    @Test
    fun `Should not recommend by no amount`() = assertFalse(
        testInstance.isRecommended("BBVA", "Payment in aliexpress with your card ending in 8850 accepted.")
    )

    @Test
    fun `Should not recommend by too long text`() = assertFalse(
        testInstance.isRecommended("BBVA", """
            Alert: Your account XXXX-XXXX was charged ${'$'}5.75 on 04 Feb 2024 at Grind & Brew Cafe. Your new balance is ${'$'}1,024.33. This purchase includes a ${'$'}0.25 eco-contribution for our new biodegradable cups. Thank you for choosing sustainable options! For queries, call 1800-XXX-XXXX. Msg&Data rates may apply. To unsubscribe, reply STOP.
        """.trimIndent())
    )

    @Test
    fun `Should not recommend by no currency`() = assertFalse(
        testInstance.isRecommended("BBVA","Payment of 4,10 in aliexpress with your card ending in 8850 accepted.")
    )

    @Test
    fun `Should not recommend by blacklist`() = assertFalse(
        testInstance.isRecommended("revolut","Payment of 4,10 EUR in aliexpress with your card ending in 8850 accepted.")
    )

    @Test
    fun `Should not recommend by blacklist phrase ignore case`() = assertFalse(
        testInstance.isRecommended("Google Wallet","Payment of 4,10 EUR in aliexpress with your card ending in 8850 accepted.")
    )
}