package fumi.day.literalmemo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskListTest {

    @Test
    fun `checks an unchecked item`() {
        assertEquals("- [x] buy milk", toggleTaskAt("- [ ] buy milk", 0))
    }

    @Test
    fun `unchecks a checked item`() {
        assertEquals("- [ ] buy milk", toggleTaskAt("- [x] buy milk", 0))
    }

    @Test
    fun `uppercase marker is treated as checked`() {
        assertEquals("- [ ] buy milk", toggleTaskAt("- [X] buy milk", 0))
    }

    @Test
    fun `toggles the item at the given index only`() {
        val markdown = """
            - [ ] one
            - [ ] two
            - [ ] three
        """.trimIndent()

        assertEquals(
            """
            - [ ] one
            - [x] two
            - [ ] three
            """.trimIndent(),
            toggleTaskAt(markdown, 1)
        )
    }

    @Test
    fun `counts nested and ordered items in document order`() {
        val markdown = """
            # Shopping

            - [ ] fruit
              - [ ] apples
            1. [ ] pay
        """.trimIndent()

        assertEquals(
            """
            # Shopping

            - [ ] fruit
              - [x] apples
            1. [ ] pay
            """.trimIndent(),
            toggleTaskAt(markdown, 1)
        )
        assertEquals(
            """
            # Shopping

            - [ ] fruit
              - [ ] apples
            1. [x] pay
            """.trimIndent(),
            toggleTaskAt(markdown, 2)
        )
    }

    @Test
    fun `skips checkboxes inside fenced code blocks`() {
        val markdown = """
            ```
            - [ ] sample from a guide
            ```
            - [ ] real task
        """.trimIndent()

        assertEquals(
            """
            ```
            - [ ] sample from a guide
            ```
            - [x] real task
            """.trimIndent(),
            toggleTaskAt(markdown, 0)
        )
    }

    @Test
    fun `leaves other text untouched`() {
        val markdown = "intro [x] not a task\n- [ ] task\ntrailing"

        assertEquals("intro [x] not a task\n- [x] task\ntrailing", toggleTaskAt(markdown, 0))
    }

    @Test
    fun `returns null when the index has no task`() {
        assertNull(toggleTaskAt("- [ ] only one", 1))
        assertNull(toggleTaskAt("no tasks here", 0))
        assertNull(toggleTaskAt("- [ ] only one", -1))
    }
}
