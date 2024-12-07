package simpret.interpreter

import simpret.parser._
import simpret.errors._


case class EvaluationException(private val message: String, private val x: AST,
  private val cause: Throwable = None.orNull)
    extends Exception(message, cause)


object Interpreter {
  def errFun(msg: String, x: AST) = throw new EvaluationException(msg, x)

  /* function for defining which AST elements are values */
  def isvalue(x: AST) = x match {
    case Variable(_) => true
    case BoolLit(_) => true
    case IntLit(_) => true
    case LamExp(_, _) => true
    case _ => false
  }

  /* computes free variables set for an expression */
  def free_variables(t: AST) : List[String] = {
    t match {
      case Variable(x) => 
        List(x)
      case LamExp(x, t) => 
        free_variables(t) diff List(x)
      case AppExp(t1, t2) => 
        free_variables(t1) ++ free_variables(t2)
      case _ =>
        List()
    }
  }

  /* performs alpha conversion, i.e. renames bound variable */
  def alpha_conversion(t: LamExp): LamExp = {
    t match {
      case LamExp(y, t1) => LamExp(y + "1", aca(t1, y, y + "1"))
    }
  }

  /* alpha conversion auxiliary function */
  def aca(t: AST, old_id: String, new_id: String): AST = {
    t match {
      case Variable(x) if x == old_id =>
        Variable(new_id)
      case CondExp(c, e1, e2) =>
        CondExp(aca(c, old_id, new_id), aca(c, old_id, new_id), aca(c, old_id, new_id))
      case IsZeroExp(e) =>
        IsZeroExp(aca(e, old_id, new_id))
      case PlusExp(e1, e2) =>
        PlusExp(aca(e1, old_id, new_id), aca(e2, old_id, new_id))
      case LamExp(y, t1) if (free_variables(t) contains old_id) =>
        LamExp(y, aca(t1, old_id, new_id))
      case AppExp(t1, t2) if (free_variables(t) contains old_id) =>
        AppExp(aca(t1, old_id, new_id), aca(t2, old_id, new_id))
      case _ => t
    }
  }

  /* performs capture-avoiding substitution */
  def cas(t: AST, x: String, s: AST) : AST = {
    t match {
      case Variable(y) if (y == x) =>
        s
      case Variable(y) if (y != x) =>
        Variable(y)

      case CondExp(c, e1, e2) =>
        CondExp(cas(c, x, s), cas(e1, x, s), cas(e2, x, s))
      case IsZeroExp(e) =>
        IsZeroExp(cas(e, x, s))
      case PlusExp(e1, e2) =>
        PlusExp(cas(e1, x, s), cas(e2, x, s))

      case LamExp(y, t1) if (y == x) => 
        LamExp(y, t1)
      case LamExp(y, t1) if (y != x && !(free_variables(s) contains y)) =>
        LamExp(y, cas(t1, x, s))
      case LamExp(y, t1) if (y != x && (free_variables(s) contains y)) =>
        cas(alpha_conversion(LamExp(y, t1)), x, s)

      case AppExp(t1, t2) =>
        AppExp(cas(t1, x, s), cas(t2, x, s))

      case _ =>
        t
    }
  }

  /* evaluation function for taking one step at a time */
  def step(tree: AST): Option[AST] = {
    tree match {
      // E-IfTrue
      case CondExp(BoolLit(true), t2, t3) =>
        Some(t2)
      // E-IfFalse
      case CondExp(BoolLit(false), t2, t3) =>
        Some(t3)
      // E-If
      case CondExp(t1, t2, t3) if !isvalue(t1) =>
        Some(CondExp(step(t1).get, t2, t3))
      // E-IsZeroZero
      case IsZeroExp(IntLit(0)) =>
        Some(BoolLit(true))
      // E-IsZeroNonZero
      case IsZeroExp(IntLit(c)) if c != 0 =>
        Some(BoolLit(false))
      // E-IsZero
      case IsZeroExp(t1) if !isvalue(t1) =>
        Some(IsZeroExp(step(t1).get))
      // E-Add
      case PlusExp(IntLit(c1), IntLit(c2)) =>
        Some(IntLit(c1 + c2))
      // E-AddRight
      case PlusExp(IntLit(c1), t2) if !isvalue(t2) =>
        Some(PlusExp(IntLit(c1), step(t2).get))
      // E-AddLeft
      case PlusExp(t1, t2) if !isvalue(t1) =>
        Some(PlusExp(step(t1).get, t2))
      // E-App1
      case AppExp(t1, t2) if !isvalue(t1) =>
        Some(AppExp(step(t1).get, t2))
      // E-App2
      case AppExp(t1, t2) if !isvalue(t2) =>
        Some(AppExp(t1, step(t2).get))
      // E-AppAbs
      case AppExp(LamExp(x, t1), v2) =>
        Some(cas(t1, x, v2))
      case _ => None
    }
  }

  /* evaluation function to iterate the steps of evaluation */
  def eval(x: AST): AST = {
    println(x)
    step(x) match {
      case None => x
      case Some(x1) => eval(x1)
    }
  }

  /* function to apply the interpreter */
  def apply(x: AST): Either[EvaluationError, AST] = {
    try {
      Right(eval(x))
    } catch {
      case EvaluationException (msg, xe, _) =>
        val msg2 = msg + " -> \r\n" + ASTPrinter.convToStr(xe)
        Left(EvaluationError(Location.fromPos(xe.pos), msg2))
    }
  }
}
