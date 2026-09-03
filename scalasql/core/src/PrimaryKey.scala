package scalasql.core

object PrimaryKey:
  opaque type Is[TableWithPk[_[_]], Pk] <: Pk = Pk
  inline def apply[TableWithPk[_[_]], Pk](pk: Pk): Is[TableWithPk, Pk] = pk

  given [TableWithPk[_[_]], Pk](using pkTm: TypeMapper[Pk]): TypeMapper[Is[TableWithPk, Pk]] =
    pkTm.asInstanceOf

type PrimaryKey[Table[_[_]], Pk] = PrimaryKey.Is[Table, Pk]
