package org.nypl.simplified.tests.mime

import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.mime.MIMELexerResult
import org.nypl.simplified.mime.MIMELexerType
import org.nypl.simplified.mime.MIMEToken
import org.nypl.simplified.mime.MIMEToken.*
import org.nypl.simplified.mime.MIMEToken.MIMETextToken.*

abstract class MIMELexerContract {

  abstract fun lexer(text: String): MIMELexerType

  @Test
  fun testEmpty()
  {
    val lexer = lexer("")
    val token = lexer.token()
    Assert.assertEquals(MIMELexerResult.Success(EOF(0)), token)
  }

  @Test
  fun testType()
  {
    val lexer = lexer("type")

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(0, "type")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(4)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(4)), token)
    }
  }

  @Test
  fun testSemicolon0()
  {
    val lexer = lexer(";")

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Semicolon(0)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(1)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(1)), token)
    }
  }

  @Test
  fun testSemicolon1()
  {
    val lexer = lexer(";;")

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Semicolon(0)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Semicolon(1)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(2)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(2)), token)
    }
  }

  @Test
  fun testSlashSemicolon0()
  {
    val lexer = lexer("/;/")

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Slash(0)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Semicolon(1)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(2, "/")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(3)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(3)), token)
    }
  }

  @Test
  fun testEquals()
  {
    val lexer = lexer("=")

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Equals(0)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(1)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(1)), token)
    }
  }

  @Test
  fun testTypeSubtype()
  {
    val lexer = lexer("type/subtype")

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(0, "type")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Slash(4)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(5, "subtype")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(12)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(12)), token)
    }
  }

  @Test
  fun testQuoted()
  {
    val lexer = lexer("\"type/subtype\"")

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Quoted(1, "type/subtype")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(14)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(14)), token)
    }
  }

  @Test
  fun testTextPlain0()
  {
    val lexer = lexer("text/plain;charset=utf-8")

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(0, "text")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Slash(4)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(5, "plain")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Semicolon(10)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(11, "charset")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Equals(18)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(19, "utf-8")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(24)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(24)), token)
    }
  }

  @Test
  fun testTextPlain1()
  {
    val lexer = lexer("text/plain;charset=\"utf-8\"")

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(0, "text")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Slash(4)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(5, "plain")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Semicolon(10)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(11, "charset")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Equals(18)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Quoted(20, "utf-8")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(26)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(26)), token)
    }
  }

  @Test
  fun testOPDS0()
  {
    val lexer = lexer("text/html;profile=http://librarysimplified.org/terms/profiles/streaming-media")

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(0, "text")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Slash(4)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(5, "html")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Semicolon(9)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(10, "profile")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Equals(17)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(Text(18, "http://librarysimplified.org/terms/profiles/streaming-media")), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(77)), token)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Success(EOF(77)), token)
    }
  }

  @Test
  fun testQuotedUnexpectedEOF()
  {
    val lexer = lexer("\"")

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Failure::class.java, token.javaClass)
    }

    run {
      val token = lexer.token()
      Assert.assertEquals(MIMELexerResult.Failure::class.java, token.javaClass)
    }
  }
}