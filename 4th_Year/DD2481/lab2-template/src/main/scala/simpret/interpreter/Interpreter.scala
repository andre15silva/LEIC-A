package simpret.interpreter

import simpret.parser._
import simpret.errors._


abstract case class EvaluationException(val message: String, val x: AST,
                                     private val cause: Throwable = None.orNull)
  extends Exception(message, cause)

final class VariableCapturedEvaluationException(val var_id: String, val subst_s: AST,
                                                private val cause: Throwable = None.orNull)
  extends EvaluationException("variable (" + var_id + ") has been captured during substitution", subst_s, cause)

object Interpreter {
  def errVarCap(var_id: String, x: AST) = throw new VariableCapturedEvaluationException(var_id, x)

  /* function for defining which AST elements are values */
  def isvalue(x: AST): Boolean = x match {
    case BoolLit(_) => true
    case IntLit(_) => true
    case LamExp(_,_,_) => true
    case NilExp(_) => true
    case ConsExp(eh, et) => isvalue(eh) & isvalue(et)
    case TupleExp(el) => el.forall(isvalue)
    case RecordExp(em) => em.values.forall(isvalue)
    case _ => false
  }

  /* function for determining the free variables of an expression */
  def freevars(t: AST) : List[String] = t match {
    case Variable(x) => 
      List(x)
    case LamExp(x, ty, e) => 
      freevars(e) diff List(x)
    case AppExp(t1, t2) => 
      freevars(t1) ++ freevars(t2)
    case _ =>
      List()
  }

  /* performs alpha conversion, i.e. renames bound variable */
  // TODO: Make this work for recursive cases
  def alpha_conversion(t: LamExp): LamExp = {
    t match {
      case LamExp(y, ty, e) => LamExp(y + "1", ty, aca(e, y, y + "1"))
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
      case LamExp(y, ty, e) if (freevars(e) contains old_id) =>
        LamExp(y, ty, aca(e, old_id, new_id))
      case AppExp(t1, t2) if (freevars(t) contains old_id) =>
        AppExp(aca(t1, old_id, new_id), aca(t2, old_id, new_id))
      case _ => t
    }
  }

  /* function for carrying out a substitution */
 def cas(t: AST, x: String, s: AST) : AST = t match {
    case Variable(y) if (y == x) =>
      s
    case Variable(y) if (y != x) =>
      Variable(y)

    case CondExp(c, e1, e2) =>
      CondExp(cas(c, x, s), cas(e1, x, s), cas(e2, x, s))
    case PlusExp(e1, e2) =>
      PlusExp(cas(e1, x, s), cas(e2, x, s))
    case UMinExp(e1) =>
      UMinExp(cas(e1, x, s))
    case LtExp(e1, e2) =>
      LtExp(cas(e1, x, s), cas(e2, x, s))

    case LamExp(y, ty, e) if (y == x) => 
      LamExp(y, ty, e)
    case LamExp(y, ty, e) if (y != x && !(freevars(s) contains y)) =>
      LamExp(y, ty, cas(e, x, s))
    case LamExp(y, ty, e) if (y != x && (freevars(s) contains y)) =>
      cas(alpha_conversion(LamExp(y, ty, e)), x, s)

    case AppExp(t1, t2) =>
      AppExp(cas(t1, x, s), cas(t2, x, s))

    // If id == x, we replace only on the current let
    case LetExp(id, e1, e2) if (id == x) => {
      LetExp(id, cas(e1, x, s), e2)
    }
    // Else we propagate the replacement
    case LetExp(id, e1, e2) if (id != x) => {
      LetExp(id, cas(e1, x, s), cas(e2, x, s))
    }

    case FixAppExp(e) =>
      FixAppExp(cas(e, x, s))

    case TupleExp(el) =>
      TupleExp(el.map(cas(_, x, s)))
    case ProjTupleExp(e, i) =>
      ProjTupleExp(cas(e, x, s), i)

    case RecordExp(em) =>
      RecordExp(em map { case (k, v) => (k, cas(v, x, s)) })
    case ProjRecordExp(e, l) =>
      ProjRecordExp(cas(e, x, s), l)

    case ConsExp(eh, et) =>
      ConsExp(cas(eh, x, s), cas(et, x, s))
    case IsNilExp(e) =>
      IsNilExp(cas(e, x, s))
    case HeadExp(e) =>
      HeadExp(cas(e, x, s))
    case TailExp(e) =>
      TailExp(cas(e, x, s))

    case _ =>
      t
  }

  /* evaluation function for taking one step at a time */
  def step(tree: AST): Option[AST] = {
//    println(ASTPrinter.convToStr(tree))
//    println(tree)
//    println("=================")
    tree match {
      // E-IfTrue
      case CondExp(BoolLit(true), t2, t3) => 
        Some(t2)
      // E-IfFalse
      case CondExp(BoolLit(false), t2, t3) =>
        Some(t3)
      // E-If
      case CondExp(t1, t2, t3) if !isvalue(t1) => step(t1) match {
        case Some(t1_) => Some(CondExp(step(t1).get, t2, t3))
        case _ => None
      }

      // E-Add
      case PlusExp(IntLit(c1), IntLit(c2)) =>
        Some(IntLit(c1 + c2))
      // E-AddRight
      case PlusExp(IntLit(c1), t2) => step(t2) match {
        case Some(t2_) => Some(PlusExp(IntLit(c1), t2_))
        case _ => None
      }
      // E-AddLeft
      case PlusExp(t1, t2) => step(t1) match {
        case Some(t1_) => Some(PlusExp(t1_, t2))
        case _ => None
      }

      // E-MinusVal
      case UMinExp(IntLit(c)) =>
        Some(IntLit(-c))
      // E-Minus
      case UMinExp(t) => step(t) match {
        case Some(t_) => Some(UMinExp(t_))
        case _ => None
      }

      // E-LessThan
      case LtExp(IntLit(c1), IntLit(c2)) =>
        Some(BoolLit(c1 < c2))
      // E-LessThanRight
      case LtExp(IntLit(c1), t2) => step(t2) match {
        case Some(t2_) => Some(LtExp(IntLit(c1), t2_))
        case _ => None
      }
      // E-LessThanLeft
      case LtExp(t1, t2) => step(t1) match {
        case Some(t1_) => Some(LtExp(step(t1).get, t2))
        case _ => None
      }

      // E-App1
      case AppExp(t1, t2) if !isvalue(t1) => step(t1) match {
        case Some(t1_) => Some(AppExp(t1_, t2))
        case _ => None
      }
      // E-App2
      case AppExp(t1, t2) if !isvalue(t2) => step(t2) match {
        case Some(t2_) => Some(AppExp(t1, t2_))
        case _ => None
      }
      // E-AppAbs
      case AppExp(LamExp(x, ty, e), v2) =>
        Some(cas(e, x, v2))

      // E-LetV
      case LetExp(id, e1, e2) if isvalue(e1) =>
        Some(cas(e2, id, e1))
      // E-Let
      case LetExp(id, e1, e2) if !isvalue(e1) => step(e1) match {
        case Some(e1_) => Some(LetExp(id, e1_, e2))
        case _ => None
      }

      // E-FixBeta
      case FixAppExp(LamExp(id, ty, e)) =>
        Some(cas(e, id, tree))
      // E-Fix
      case FixAppExp(e) => step(e) match {
        case Some(e_) => Some(FixAppExp(e_))
        case _ => None
      }


      
      // E-ProjTuple
      case ProjTupleExp(TupleExp(el), i) if el.forall(isvalue) =>
        Some(el(i-1))
      // E-Proj
      case ProjTupleExp(e, i) => step(e) match {
        case Some(e_) => Some(ProjTupleExp(e_, i))
        case _ => None
      }
      // E-Tuple
      case TupleExp(el) if !el.forall(isvalue) => {
        val i = el.map(isvalue).indexOf(false)
        step(el(i)) match {
          case Some(eli) => Some(TupleExp(el.updated(i, eli)))
          case _ => None
        }
      }


      // E-ProjRcd
      case ProjRecordExp(RecordExp(em), l) if em.values.forall(isvalue) =>
        Some(em(l))
      // E-Proj
      case ProjRecordExp(t, l)  => step(t) match {
        case Some(t_) => Some(ProjRecordExp(t_, l))
        case _ => None
      }
      // E-Rcd
      // Note: This isn't well defined in the rule, because there is supposed to be an ordering (?)
      case RecordExp(em) if !em.values.forall(isvalue) => {
        em.find(x => !isvalue(x._2)).map(_._1) match {
          case Some(l) => step(em(l)) match {
            case Some(eml) => Some(RecordExp(em.updated(l, eml)))
            case _ => None
          }
          // It never happens because there's always one that isn't a value
          case _ => None
        }
      }


      // E-Cons1
      case ConsExp(eh, et) if !isvalue(eh) => step(eh) match {
        case Some(eh_) => Some(ConsExp(eh_, et))
        case _ => None
      }
      // E-Cons2
      case ConsExp(eh, et) if !isvalue(et) => step(et) match {
        case Some(et_) => Some(ConsExp(eh, et_))
        case _ => None
      }

      // E-IsNilNil
      case IsNilExp(NilExp(_)) =>
        Some(BoolLit(true))
      // E-IsNilCons
      case IsNilExp(ConsExp(v1,v2)) if isvalue(v1) && isvalue(v2) =>
        Some(BoolLit(false))
      // E-IsNil
      case IsNilExp(t) if !isvalue(t) => step(t) match {
        case Some(t_) => Some(IsNilExp(t_))
        case _ => None
      }

      // E-HeadCons
      case HeadExp(ConsExp(v1,v2)) if isvalue(v1) && isvalue(v2) =>
        Some(v1)
      // E-Head
      case HeadExp(t) if !isvalue(t) => step(t) match {
        case Some(t_) => Some(HeadExp(t_))
        case _ => None
      }

      // E-TailCons
      case TailExp(ConsExp(v1,v2)) if isvalue(v1) && isvalue(v2) =>
        Some(v2)
      // E-Tail
      case TailExp(t) if !isvalue(t) => step(t) match {
        case Some(t_) => Some(TailExp(t_))
        case _ => None
      }


      case _ => None
    }
  }

  /* evaluation function to iterate the steps of evaluation */
  def eval(x: AST): AST = step(x) match {
    case None => x
    case Some(x1) => eval(x1)
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
