package com.androidessence.cashcaretaker.refresh

import android.os.Bundle
import android.support.v7.widget.Toolbar
import com.androidessence.cashcaretaker.R

class AddAccountActivity : CoreActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_account_refresh)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
    }
}
