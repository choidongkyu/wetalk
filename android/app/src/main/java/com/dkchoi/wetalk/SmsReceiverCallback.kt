package com.dkchoi.wetalk

interface SmsReceiverCallback {
    /**
     * SMS 수신시 호출되는 콜백 메서드
     * @param senderNo 발신 번호
     * @param messageBody 발신 메시지 문자열
     */
    fun onReceive(senderNo: String?, messageBody: String?)
}