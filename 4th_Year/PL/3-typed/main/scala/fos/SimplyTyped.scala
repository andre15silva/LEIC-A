package fos

import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import scala.util.parsing.input._
import java.lang.annotation.Native

/** This object implements a parser and evaluator for the
 *  simply typed lambda calculus found in Chapter 9 of
 *  the TAPL book.
 */
object SimplyTyped extends StandardTokenParsers {
  lexical.delimiters ++= List("(", ")", "\\", ".", ":", "=", "->", "{", "}", ",", "*")
  lexical.reserved   ++= List("Bool", "Nat", "true", "false", "if", "then", "else", "succ",
                              "pred", "iszero", "let", "in", "fst", "snd")

  /** t ::=          "true"
   *               | "false"
   *               | number
   *               | "succ" t
   *               | "pred" t
   *               | "iszero" t
   *               | "if" t "then" t "else" t
   *               | ident
   *               | "\" ident ":" T "." t
   *               | t t
   *               | "(" t ")"
   *               | "let" ident ":" T "=" t "in" t
   *               | "{" t "," t "}"
   *               | "fst" t
   *               | "snd" t
   */

  def term: Parser[Term] = applicationParser | secundaryTermParser

  def secundaryTermParser: Parser[Term] = {
    trueParser | falseParser | numberParser | succParser | predParser | isZeroParser | ifParser | variableParser | abstractionParser | parenthesisParser | letParser | pairParser | fstParser | sndParser
  }

  def parseApplicationTerm: Term ~ List[Term] => Term = {
    case i ~ ps => (ps.foldLeft(i))({ (t1, t2) => App(t1, t2) })
  }

  def applicationParser: Parser[Term] = {
    secundaryTermParser ~ rep1(secundaryTermParser) ^^ parseApplicationTerm
  }

  def pairParser: Parser[Term] =
    "{" ~ term ~ "," ~ term ~ "}" ^^ {
      case "{" ~ t1 ~ "," ~ t2 ~ "}" => TermPair(t1, t2)
    }

  def fstParser: Parser[Term] =
    "fst" ~> term ^^ (t => First(t))

  def sndParser: Parser[Term] =
    "snd" ~> term ^^ (t => Second(t))

  def letParser: Parser[Term] =
    "let" ~ ident ~ ":" ~ typeParser ~ "=" ~ term ~ "in" ~ term ^^ {
      case "let" ~ x ~ ":" ~ tp ~ "=" ~ t1 ~ "in" ~ t2 => App(Abs(x, tp, t1), t2)
    }

  def trueParser: Parser[Term] =
    "true" ^^ (t => True)

  def falseParser: Parser[Term] =
    "false" ^^ (t => False)

  def numberParser: Parser[Term] =
    numericLit ^^ (lit => churchNumeralConstructor(lit.toInt))

  def succParser: Parser[Term] =
    "succ" ~> term ^^ (t => Succ(t))

  def predParser: Parser[Term] =
    "pred" ~> term ^^ (t => Pred(t))

  def isZeroParser: Parser[Term] =
    "iszero" ~> term ^^ (t => IsZero(t))

  def ifParser: Parser[Term] =
    "if" ~ term ~ "then" ~ term ~ "else" ~ term ^^ {
      case "if" ~ cond ~ "then" ~ t1 ~ "else" ~ t2 => If(cond, t1, t2)
    }

  def variableParser: Parser[Term] =
    ident ^^ (Var(_))
  
  def abstractionParser: Parser[Term] =
    "\\" ~ ident ~ ":" ~ typeParser ~ "." ~ term ^^ {case "\\" ~ id ~ ":" ~ tp ~ "." ~ t => Abs(id, tp, t)}

  def parenthesisParser: Parser[Term] =
    "(" ~> term <~ ")"


  def typeParser: Parser[Type] = secundaryTypeParser ~ rep(functionTypeParser | pairTypeParser) ^^ parseType

  def secundaryTypeParser: Parser[Type] = boolTypeParser | natTypeParser | parenthesisTypeParser

  def boolTypeParser: Parser[Type] =
    "Bool" ^^ (t => TypeBool)

  def natTypeParser: Parser[Type] =
    "Nat" ^^ (t => TypeNat)

  def parenthesisTypeParser: Parser[Type] =
    "(" ~> typeParser <~ ")"

  def parseType: Type ~ List[String ~ Type] => Type = {
    case i ~ ps => (ps.foldRight(i)) ({
      case ("->" ~ t2, t1) => TypeFun(t1, t2)
      case ("*" ~ t2, t1) => TypePair(t1, t2)
    })
  }

  def pairTypeParser: Parser[String ~ Type] = "*" ~ typeParser
  def functionTypeParser: Parser[String ~ Type] = "->" ~ typeParser


  def churchNumeralConstructor(lit: Int): Term = {
    lit match {
      case 0 => Zero
      case _ => Succ(churchNumeralConstructor(lit-1))  
    }
  }

  /** Thrown when no reduction rule applies to the given term. */
  case class NoRuleApplies(t: Term) extends Exception(t.toString)

  /** Print an error message, together with the position where it occured. */
  case class TypeError(t: Term, msg: String) extends Exception(msg) {
    override def toString =
      msg + "\n" + t
  }

  /** The context is a list of variable names paired with their type. */
  type Context = List[(String, Type)]

  def getTypeFromContext(ctx: Context, name: String): Type = {
    ctx.find((pair => pair._1 == name)).get._2
  }

  def isValue(t: Term): Boolean = {
    t match {
      case True | False | Abs(_, _, _) => true
      case TermPair(t1, t2) => isValue(t1) && isValue(t2)
      case _ => isNv(t)
    }
  }


/** <p>
   *    Alpha conversion: term <code>t</code> should be a lambda abstraction
   *    <code>\x. t</code>.
   *  </p>
   *  <p>
   *    All free occurences of <code>x</code> inside term <code>t/code>
   *    will be renamed to a unique name.
   *  </p>
   *
   *  @param t the given lambda abstraction.
   *  @return  the transformed term with bound variables renamed.
   */
  def alpha(t: Abs): Abs = t match {
    case Abs(v, xType, t1) => Abs(v.concat("#"), xType, alpha_aux(t1, v.concat("#"), v))
  }

  def alpha_aux(t: Term, new_name: String, old_name: String): Term =
    t match {
      case Var(name) if (isFV(Var(name), old_name)) => Var(new_name)
      case Abs(v, xType, t1) if (isFV(t, old_name)) => Abs(v, xType, alpha_aux(t1, new_name, old_name))
      case App(t1, t2) if (isFV(t, old_name)) => App(alpha_aux(t1, new_name, old_name), alpha_aux(t2, new_name, old_name))
      case _=> t
  }

  /** Straight forward substitution method
   *  (see definition 5.3.5 in TAPL book).
   *  [x -> s]t
   *
   *  @param t the term in which we perform substitution
   *  @param x the variable name
   *  @param s the term we replace x with
   *  @return  ...
   */
  def subst(t: Term, x: String, s: Term): Term = t match {
    case Pred(t1) => Pred(subst(t1, x, s))
    case Succ(t1) => Succ(subst(t1, x, s))
    case IsZero(t1) => IsZero(subst(t1, x, s))
    case If(t1, t2, t3) => If(subst(t1, x, s), subst(t2, x, s), subst(t3, x, s))
    case First(t1) => First(subst(t1, x, s))
    case Second(t1) => Second(subst(t1, x, s))
    case TermPair(t1, t2) => TermPair(subst(t1, x, s), subst(t2, x, s))
    case Var(name) if (name == x) => s
    case Var(name) if (name != x) => Var(name)
    case Abs(v, tp, t1) if (v == x) => Abs(v, tp, t1)
    case Abs(v, tp, t1) if (v != x && (!isFV(s, v))) => Abs(v, tp, subst(t1, x, s))
    case Abs(v, tp, t1) if (v != x && isFV(s, v)) => subst(alpha(Abs(v, tp, t1)), x, s)
    case App(t1, t2) => App(subst(t1, x, s), subst(t2, x, s))
    case _ => t
  }

  def isFV(term: Term, s: String): Boolean = FVgroup(term) contains s

  def FVgroup(term: Term): List[String] = {
    val free_vars = List()
    term match {
      case Var(name) => name :: free_vars
      case Abs(v, _, t) => FVgroup(t) diff List(v)
      case App(t1, t2) => FVgroup(t1) ++ FVgroup(t2)
      case Pred(t1) => FVgroup(t1)
      case Succ(t1) => FVgroup(t1)
      case IsZero(t1) => FVgroup(t1)
      case If(t1, t2, t3) => FVgroup(t1) ++ FVgroup(t2) ++ FVgroup(t3)
      case First(t1) => FVgroup(t1)
      case Second(t1) => FVgroup(t1)
      case TermPair(t1, t2) => FVgroup(t1) ++ FVgroup(t2)
      case _ => List()
    }
  }

  def isNv(t: Term): Boolean ={
    t match {
      case Zero => true
      case Succ(x) => isNv(x)
      case _ => false
    }
  }

  /** Call by value reducer. */
  def reduce(t: Term): Term = {
    t match {
      case If(True, t1, t2) => t1
      case If(False, t1, t2) => t2
      case IsZero(Zero) => True
      case IsZero(Succ(nv)) if isNv(nv) => False
      case Pred(Zero) => Zero
      case Pred(Succ(nv)) if isNv(nv) => nv
      case App(Abs(v, _, t1), t2) if isValue(t2) => subst(t1, v, t2)
      case If(t1, t2, t3) => If(reduce(t1), t2, t3)
      case IsZero(t1) => IsZero(reduce(t1))
      case Succ(t1) => Succ(reduce(t1))
      case Pred(t1) => Pred(reduce(t1))
      case App(t1, t2) if !isValue(t1) => App(reduce(t1), t2)
      case App(v1, t2) => App(v1, reduce(t2))
      case First(TermPair(v1, v2)) if isValue(v1) && isValue(v2) => v1
      case Second(TermPair(v1, v2)) if isValue(v1) && isValue(v2) => v2
      case First(t1) => First(reduce(t1))
      case Second(t1) => Second(reduce(t1))
      case TermPair(t1, t2) if !isValue(t1) => TermPair(reduce(t1), t2)
      case TermPair(v1, t2) => TermPair(v1, reduce(t2))
      case _ => throw new NoRuleApplies(t)
    }
  }


  /** Returns the type of the given term <code>t</code>.
   *
   *  @param ctx the initial context
   *  @param t   the given term
   *  @return    the computed type
   */
  def typeof(ctx: Context, t: Term): Type = {
    t match {
      case True | False => TypeBool
      case Zero => TypeNat
      case Pred(t) if typeof(ctx,t) == TypeNat => TypeNat
      case Succ(t) if typeof(ctx,t) == TypeNat => TypeNat
      case IsZero(t) if typeof(ctx, t) == TypeNat => TypeBool
      case If(t1, t2, t3) if typeof(ctx, t1) == TypeBool && typeof(ctx, t2) == typeof(ctx, t3) => typeof(ctx, t2)
      case Var(x) if ctx.exists((pair => pair._1 == x)) => getTypeFromContext(ctx, x)
      case Abs(x, xType, t2) => TypeFun(xType, typeof((x, xType) :: ctx, t2))
      case App(t1, t2) => typeof(ctx, t1) match {
        case TypeFun(tp1, tp2) if tp1 == typeof(ctx, t2) => tp2
        case TypeFun(tp1, tp2) => throw new TypeError(t, "parameter type mismatch: expected " + tp1 + " found " + typeof(ctx, t2))
        case _ => throw new TypeError(t, "parameter type mismatch")
      }

      case TermPair(t1, t2) => TypePair(typeof(ctx, t1), typeof(ctx, t2))

      case First(t1) => typeof(ctx, t1) match {
        case TypePair(t2, _) => t2
        case _ => throw new TypeError(t, "pair type expected but " + typeof(ctx, t1) + " found")
      }

      case Second(t1) => typeof(ctx, t1) match {
        case TypePair(_, t2) => t2
        case _ => throw new TypeError(t, "pair type expected but " + typeof(ctx, t1) + " found")
      }
      case _ => throw new TypeError(t, "parameter type mismatch")
    }
  }



  /** Returns a stream of terms, each being one step of reduction.
   *
   *  @param t      the initial term
   *  @param reduce the evaluation strategy used for reduction.
   *  @return       the stream of terms representing the big reduction.
   */
  def path(t: Term, reduce: Term => Term): Stream[Term] =
    try {
      var t1 = reduce(t)
      Stream.cons(t, path(t1, reduce))
    } catch {
      case NoRuleApplies(_) =>
        Stream.cons(t, Stream.empty)
    }

  def main(args: Array[String]): Unit = {
    val stdin = new java.io.BufferedReader(new java.io.InputStreamReader(System.in))
    val tokens = new lexical.Scanner(stdin.readLine())
    phrase(term)(tokens) match {
      case Success(trees, _) =>
        try {
          println("typed: " + typeof(Nil, trees))
          for (t <- path(trees, reduce))
            println(t)
        } catch {
          case tperror: Exception => println(tperror.toString)
        }
      case e =>
        println(e)
    }
  }
}
