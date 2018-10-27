package com.coinzgame.theoxo.coinz

class Coin {
    var id : String? = null
    var currency : String? = null
    var value : String? = null

    constructor(id: String, currency: String, value: String) {
        this.id = id
        this.currency = currency
        this.value = value
    }
}