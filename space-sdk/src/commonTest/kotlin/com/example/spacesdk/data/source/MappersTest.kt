package com.example.spacesdk.data.source

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MappersTest {

    @Test
    fun stripsTagsAndDecodesEntities() {
        val html = "<a href=\"https://www.nasa.gov/\">NASA</a>, <em>Processing &amp; " +
            "<a href=\"x\">License</a>:</em> Se&#225;n Dor&#xE1;n&nbsp;&ndash; Juno"

        assertEquals("NASA, Processing & License: Seán Dorán – Juno", html.htmlToPlainText())
    }

    @Test
    fun brBecomesALineBreak() {
        assertEquals("Assembly - Rolf Olsen,\nData - Voyager 2", "Assembly - Rolf Olsen,<br>Data - Voyager 2".htmlToPlainText())
    }

    @Test
    fun apodDateCodeIsYyMmDd() {
        assertEquals("140918", LocalDate(2014, 9, 18).toApodDateCode())
        assertEquals("950616", LocalDate(1995, 6, 16).toApodDateCode())
        assertEquals("261122", LocalDate(2026, 11, 22).toApodDateCode())
    }
}
