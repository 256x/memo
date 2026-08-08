package fumi.day.literalmemo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListContinuationTest {

    /** Types a newline at the end of [text] the way the editor hands it over. */
    private fun enterAtEnd(text: String): Continuation? =
        continueListOnNewline("$text\n", text.length + 1)

    @Test
    fun `bullet item repeats its marker`() {
        val result = enterAtEnd("- milk")

        assertEquals("- milk\n- ", result?.text)
        assertEquals("- milk\n- ".length, result?.cursor)
    }

    @Test
    fun `asterisk and plus bullets keep their own character`() {
        assertEquals("* milk\n* ", enterAtEnd("* milk")?.text)
        assertEquals("+ milk\n+ ", enterAtEnd("+ milk")?.text)
    }

    @Test
    fun `task item comes back unchecked`() {
        assertEquals("- [ ] milk\n- [ ] ", enterAtEnd("- [ ] milk")?.text)
        assertEquals("- [x] milk\n- [ ] ", enterAtEnd("- [x] milk")?.text)
        assertEquals("- [X] milk\n- [ ] ", enterAtEnd("- [X] milk")?.text)
    }

    @Test
    fun `ordered item counts up and keeps its delimiter`() {
        assertEquals("1. milk\n2. ", enterAtEnd("1. milk")?.text)
        assertEquals("9) milk\n10) ", enterAtEnd("9) milk")?.text)
    }

    @Test
    fun `indentation is carried over`() {
        assertEquals("  - milk\n  - ", enterAtEnd("  - milk")?.text)
        assertEquals("\t- [ ] milk\n\t- [ ] ", enterAtEnd("\t- [ ] milk")?.text)
    }

    @Test
    fun `empty item drops the marker and the newline`() {
        val result = continueListOnNewline("- milk\n- \n", "- milk\n- \n".length)

        assertEquals("- milk\n", result?.text)
        assertEquals("- milk\n".length, result?.cursor)
    }

    @Test
    fun `empty task item drops the box too`() {
        val result = enterAtEnd("- [ ] ")

        assertEquals("", result?.text)
        assertEquals(0, result?.cursor)
    }

    @Test
    fun `continuation happens mid document`() {
        val text = "# Title\n- milk\nafter"
        val cursor = "# Title\n- milk\n".length

        val result = continueListOnNewline(text, cursor)

        assertEquals("# Title\n- milk\n- after", result?.text)
        assertEquals("# Title\n- milk\n- ".length, result?.cursor)
    }

    @Test
    fun `plain text is left alone`() {
        assertNull(enterAtEnd("just a sentence"))
        assertNull(enterAtEnd("# heading"))
        assertNull(enterAtEnd(""))
    }

    @Test
    fun `a marker with no trailing space is not a list`() {
        assertNull(enterAtEnd("-milk"))
        assertNull(enterAtEnd("1.milk"))
    }

    @Test
    fun `a cursor that is not just past a newline is ignored`() {
        assertNull(continueListOnNewline("- milk", 6))
        assertNull(continueListOnNewline("- milk\n", 0))
    }
}
