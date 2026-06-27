package scalasql

object IsCovariantHK:
  type NothingK[_] = Nothing
  type AnyK[_] = Any

  opaque type Proof[Table[_[_]]] = Table[NothingK] <:< Table[AnyK]
  inline given [Table[+_[_]]]: Proof[Table] = summon[Table[NothingK] <:< Table[AnyK]]

type IsCovariantHK[Table[_[_]]] = IsCovariantHK.Proof[Table]

extension [F[_], Table[_[_]]: IsCovariantHK](table: Table[F])
  /** Widen the table HK type if it is a supertype of `F[_]`. E.g., Table[Column] to Table[Expr] */
  inline def widenK[FF[x] >: F[x]]: Table[FF] = table.asInstanceOf[Table[FF]]

object PrimaryKey:
  opaque type Is[TableWithPk[_[_]], Pk] <: Pk = Pk
  inline def apply[TableWithPk[_[_]], Pk](pk: Pk): Is[TableWithPk, Pk] = pk

  given [TableWithPk[_[_]], Pk](using pkTm: TypeMapper[Pk]): TypeMapper[Is[TableWithPk, Pk]] =
    pkTm.asInstanceOf

type PrimaryKey[Table[_[_]], Pk] = PrimaryKey.Is[Table, Pk]
