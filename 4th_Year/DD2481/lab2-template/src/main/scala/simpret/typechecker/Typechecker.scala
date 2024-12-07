package simpret.typechecker

import simpret.parser._
import simpret.errors._

object Typechecker {
  // error handling helper functions
  def errUnknownAST(x: AST) = throw new UnknownASTTypecheckerException(x)
  def errExpectedType(ty_str: String, x: AST) = throw new NotExpectedTypeTypecheckerException(ty_str, x)
  def errVarUnbound(x: AST) = throw new VarUnboundTypecheckerException(x)
  def errAppArgument(ty_param: ASTTY, ty_arg: ASTTY, x: AST) = throw new ApplicationArgumentTypecheckerException(ty_param, ty_arg, x)
  def errBranch(ty1: ASTTY, ty2: ASTTY, x: AST) = throw new BranchMismatchTypecheckerException(ty1, ty2, x)
  def errArrowNotSame(ty_param: ASTTY, ty_res: ASTTY, x: AST) = throw new ArrowNotSameTypecheckerException(ty_param, ty_res, x)
  def errCons(eh_ty: ASTTY, et_lty: ASTTY, x: AST) = throw new ConsMismatchTypecheckerException(eh_ty, et_lty, x)
  def errProjTooSmall(x: AST) = throw new ProjectionTooSmallTypecheckerException(x)
  def errProjTooBig(length: Int, x: AST) = throw new ProjectionTooBigTypecheckerException(length, x)
  def errProjNotField(l: String, x: AST) = throw new ProjectionNotAFieldTypecheckerException(l, x)

  // the recursive typechecking relation
  def check(x: AST, env: Map[String, ASTTY] = Map.empty):ASTTY = {
//    println(ASTPrinter.convToStr(x))
//    println(x)
//    println(env)
//    println("=================")
    x match {
      // T-True
      case BoolLit(true) => BoolTy
      // T-False
      case BoolLit(false) => BoolTy
      // T-Int
      case IntLit(_) => IntTy

      // T-If
      case CondExp(c, e1, e2) =>
        if (check(c, env) != BoolTy) {
          errExpectedType("bool", c)
        } else if (check(e1, env) != check(e2, env)) {
          errBranch(check(e1, env), check(e2, env), x)
        } else {
          check(e1, env)
        }

      // T-Add
      case PlusExp(e1, e2) =>
        if (check(e1, env) != IntTy) {
          errExpectedType("int", e1)
        } else if (check(e2, env) != IntTy) {
          errExpectedType("int", e2)
        } else {
          IntTy
        }

      // T-Minus
      case UMinExp(e) =>
        if (check(e, env) != IntTy) {
          errExpectedType("int", e)
        } else {
          IntTy
        }

      // T-LessThan
      case LtExp(e1, e2) =>
        if (check(e1, env) != IntTy) {
          errExpectedType("int", e1)
        } else if (check(e2, env) != IntTy) {
          errExpectedType("int", e2)
        } else {
          BoolTy
        }

      // T-Var
      case Variable(id) =>
        if (env contains id) {
          (env get id).get
        } else {
          errVarUnbound(x)
        }

      // T-Abs
      case LamExp(id, ty, e) =>
        ArrowTy(ty, check(e, env + (id -> ty)))

      // T-App
      case AppExp(t1, t2) => check(t1, env) match {
        case ArrowTy(ty1, ty2) =>
          if (ty1 != check(t2, env)) {
            errAppArgument(ty1, check(t2, env), x)
          } else {
            ty2
          }
        case _ => errExpectedType("arrow", t1)
      }

      // T-Let
      case LetExp(id, e1, e2) =>
        check(e2, env + (id -> check(e1, env)))

      // T-Fix
      case FixAppExp(e) => check(e, env) match {
        case ArrowTy(ty1, ty2) if ty1 == ty2 => ty1
        case _ => errExpectedType("arrow", e)
      }


      // T-Tuple
      case TupleExp(el) =>
        TupleTy(el.map(check(_, env)))
      // T-Proj
      case ProjTupleExp(el, i) => check(el, env) match {
        case TupleTy(el_) => 
          if (i <= 0) {
            errProjTooSmall(x)
          } else if (i > el_.length) {
            errProjTooBig(el_.length, x)
          } else {
            el_(i-1)
          }
        case _ => errExpectedType("tuple", el)
      }


      // T-Rcd
      case RecordExp(em) =>
        RecordTy(em map { case (k, v) => (k, check(v, env)) })
      // T-Proj
      case ProjRecordExp(e, l) => check(e, env) match {
        case RecordTy(e_) =>
          if (!e_.contains(l)) {
            errProjNotField(l, x)
          } else {
            e_(l)
          }
        case _ => errExpectedType("record", e)
      }

      
      // T-Nil
      case NilExp(ty) =>
        ListTy(ty)
      // T-Cons
      case ConsExp(eh, et) => check(et, env) match {
        case ListTy(ty) =>
          if (ty != check(eh, env)) {
            errCons(check(eh, env), ty, x)
          } else {
            ListTy(ty)
          }
        case _ => errExpectedType("list", et)
      }
      // T-IsNil
      case IsNilExp(e) => check(e, env) match {
        case ListTy(_) => BoolTy
        case _ => errExpectedType("list", e)
      }
      // T-Head
      case HeadExp(e) => check(e, env) match {
        case ListTy(ty) => ty
        case _ => errExpectedType("list", e)
      }
      // T-Tail
      case TailExp(e) => check(e, env) match {
        case ListTy(ty) => ListTy(ty)
        case _ => errExpectedType("list", e)
      }


      case _  => errUnknownAST(x)
    }
  }

  /* function to apply the interpreter */
  def apply(x: AST): Either[TypecheckingError, Unit] = {
    try {
      check(x)
      Right(Unit)
    } catch {
      case ex: TypecheckerException =>
        val msg = ex.message
        val x = ex.x
        val msg2 = msg + " -> \r\n" + ASTPrinter.convToStr(x)
        Left(TypecheckingError(Location.fromPos(x.pos), msg2))
    }
  }
}
