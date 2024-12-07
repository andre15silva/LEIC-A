package fos

import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import scala.util.parsing.input._
import java.security.KeyStore.TrustedCertificateEntry

/** This object implements a parser and evaluator for the
 *  untyped lambda calculus found in Chapter 5 of
 *  the TAPL book.
 */
object Untyped extends StandardTokenParsers {
  lexical.delimiters ++= List("(", ")", "\\", ".")
  import lexical.Identifier

  /** t ::= x
          | '\' x '.' t
          | t t
          | '(' t ')'
   */
  def parseList: Term ~ List[Term] => Term = {
    case i ~ ps => (i/:ps)({ (t1, t2) => App(t1, t2) })
  }

  def secondary_term: Parser[Term] = variable | abstraction | parenthesis

  def term: Parser[Term] = application

  def variable: Parser[Term] =
    ident ^^ {case t => Var(t)}

  def abstraction: Parser[Term] =
    "\\" ~ ident ~ "." ~ term ^^ {case "\\" ~ id ~ "." ~ t => Abs(id, t)}

  def parenthesis: Parser[Term] =
    "(" ~> term <~ ")"

  def application: Parser[Term] = secondary_term ~ rep(secondary_term) ^^ parseList

  def is_FV(term: Term, s: String): Boolean = FV_group(term) contains s

  def FV_group(term: Term): List[String] = {
    val free_vars = List()
    term match {
      case Var(name) => name :: free_vars
      case Abs(v, t) => FV_group(t) diff List(v)
      case App(t1, t2) => FV_group(t1) ++ FV_group(t2)
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
    case Abs(v, t1) => Abs(v.concat("1"), alpha_aux(t1, v.concat("1"), v))
  }

  def alpha_aux(t: Term, new_name: String, old_name: String): Term =
    t match {
      case Var(name) if (is_FV(Var(name), old_name)) => Var(new_name)
      case Abs(v, t1) if (is_FV(Abs(v, t1), old_name)) => Abs(v, alpha_aux(t1, new_name, old_name))
      case App(t1, t2) if (is_FV(App(t1, t2), old_name)) => 
        App(alpha_aux(t1, new_name, old_name), alpha_aux(t2, new_name, old_name))
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
    case Var(name) if (name == x) => s
    case Var(name) if (name != x) => Var(name)
    case Abs(v, t1) if (v == x) => Abs(v, t1)
    case Abs(v, t1) if (v != x && (!is_FV(s, v))) => Abs(v, subst(t1, x, s))
    case Abs(v, t1) if (v != x && is_FV(s, v)) => subst(alpha(Abs(v, t1)), x, s)
    case App(t1, t2) => App(subst(t1, x, s), subst(t2, x, s))
  }

  /** Term 't' does not match any reduction rule. */
  case class NoReductionPossible(t: Term) extends Exception(t.toString)

  /** Normal order (leftmost, outermost redex first).
   *
   *  @param t the initial term
   *  @return  the reduced term
   */
  def reduceNormalOrder(t: Term): Term = t match {
    case App(Abs(v, t1), t2) => subst(t1, v, t2)
    case App(t1, t2) if (is_normal_reducible(t1)) => App(reduceNormalOrder(t1), t2)
    case App(t1, t2) if (is_normal_reducible(t2)) => App(t1, reduceNormalOrder(t2))
    case Abs(v, t1) => Abs(v, reduceNormalOrder(t1))
    case _ => throw new NoReductionPossible(t)
  }

  def is_normal_reducible(t: Term): Boolean = {
    try{
      reduceNormalOrder(t) match {
        case _ => true
      }
    } catch {
      case e: NoReductionPossible => false
    }
  }

  def is_call_by_value_reducible(t: Term): Boolean = {
    try{
      reduceCallByValue(t) match {
        case _ => true
      }
    } catch {
      case e: NoReductionPossible => false
    }
  }

  def is_function(t: Term): Boolean = {
    t match {
      case Abs(_, _) => true
      case _ => false
    }
  }

  /** Call by value reducer. */
  def reduceCallByValue(t: Term): Term = t match {
    case App(t1, t2) if is_call_by_value_reducible(t1) => App(reduceCallByValue(t1), t2)
    case App(t1, t2) if is_function(t1) && is_call_by_value_reducible(t2) => App(t1, reduceCallByValue(t2))
    case App(Abs(v, t1), t2) if is_function(t2) => subst(t1, v, t2)
    case _ => throw new NoReductionPossible(t)
  }

  /** Returns a stream of terms, each being one step of reduction.
   *
   *  @param t      the initial term
   *  @param reduce the method that reduces a term by one step.
   *  @return       the stream of terms representing the big reduction.
   */
  def path(t: Term, reduce: Term => Term): Stream[Term] =
    try {
      var t1 = reduce(t)
      Stream.cons(t, path(t1, reduce))
    } catch {
      case NoReductionPossible(_) =>
        Stream.cons(t, Stream.empty)
    }

  def main(args: Array[String]): Unit = {
    val stdin = new java.io.BufferedReader(new java.io.InputStreamReader(System.in))
    val tokens = new lexical.Scanner(stdin.readLine())
    phrase(term)(tokens) match {
      case Success(trees, _) =>
        println("normal order: ")
        for (t <- path(trees, reduceNormalOrder))
          println(t)
        println("call-by-value: ")
        for (t <- path(trees, reduceCallByValue))
          println(t)

      case e =>
        println(e)
    }
  }
}
