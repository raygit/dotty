class Context

object Test {

  def transform()(implicit ctx: Context) = {
    inline def withLocalOwner[T](op: Context => T) = op(ctx)

    withLocalOwner { implicit ctx => }

  }
}
