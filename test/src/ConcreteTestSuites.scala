package usql
import operations.{ExprBooleanOpsTests, ExprIntOpsTests, ExprSeqNumericOpsTests, ExprSeqOpsTests, ExprStringOpsTests}
import query.{InsertTests, SelectTests, SubQueryTests, UpdateTests}

package mysql{
  object ExprBooleanOpsTests extends ExprBooleanOpsTests with MySqlSuite
  object ExprExprIntOpsTests extends ExprIntOpsTests with MySqlSuite
  object ExprSeqNumericOpsTests extends ExprSeqNumericOpsTests with MySqlSuite
  object ExprSeqOpsTests extends ExprSeqOpsTests with MySqlSuite
  object ExprStringOpsTests extends ExprStringOpsTests with MySqlSuite
  object InsertTests extends InsertTests with MySqlSuite
  object SelectTests extends SelectTests with MySqlSuite
  object SubQueryTests extends SubQueryTests with MySqlSuite
  object UpdateTests extends UpdateTests with MySqlSuite
}

package postgres{
  object ExprBooleanOpsTests extends ExprBooleanOpsTests with PostgresSuite
  object ExprExprIntOpsTests extends ExprIntOpsTests with PostgresSuite
  object ExprSeqNumericOpsTests extends ExprSeqNumericOpsTests with PostgresSuite
  object ExprSeqOpsTests extends ExprSeqOpsTests with PostgresSuite
  object ExprStringOpsTests extends ExprStringOpsTests with PostgresSuite
  object InsertTests extends InsertTests with PostgresSuite
  object SelectTests extends SelectTests with PostgresSuite
  object SubQueryTests extends SubQueryTests with PostgresSuite
  object UpdateTests extends UpdateTests with PostgresSuite
}

package sqlite{
  object ExprBooleanOpsTests extends ExprBooleanOpsTests with SqliteSuite
  object ExprIntOpsTests extends ExprIntOpsTests with SqliteSuite
  object ExprSeqNumericOpsTests extends ExprSeqNumericOpsTests with SqliteSuite
  object ExprSeqOpsTests extends ExprSeqOpsTests with SqliteSuite
  object ExprStringOpsTests extends ExprStringOpsTests with SqliteSuite
  object InsertTests extends InsertTests with SqliteSuite
  object SelectTests extends SelectTests with SqliteSuite
  object SubQueryTests extends SubQueryTests with SqliteSuite
  object UpdateTests extends UpdateTests with SqliteSuite
}