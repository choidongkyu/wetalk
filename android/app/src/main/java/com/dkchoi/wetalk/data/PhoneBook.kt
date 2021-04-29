package com.dkchoi.wetalk.data

//전화번호부 데이터
class PhoneBook {
    var id: String? = null
    var name: String? = null
    var tel: String? = null

    constructor()

    constructor(id: String?, name: String?, tel: String?) {
        this.id = id
        this.name = name
        this.tel = tel
    }
}