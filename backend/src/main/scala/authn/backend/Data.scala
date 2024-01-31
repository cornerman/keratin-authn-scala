package authn.backend

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import scala.annotation.nowarn

case class TokenPayload(
  sub: String
) {
  def id = sub.toInt
}

object TokenPayload {
  implicit val Codec: JsonValueCodec[TokenPayload] = JsonCodecMaker.make
}

case class Account(
  id: Int,
  username: String,
  locked: Boolean,
  deleted: Boolean,
)
object Account {
  implicit val Codec: JsonValueCodec[Account] = JsonCodecMaker.make
}

case class AccountImport(
  username: String,
  password: String,
  locked: Option[Boolean] = None,
)

object AccountImport {
  implicit val Codec: JsonValueCodec[AccountImport] = JsonCodecMaker.make
}

case class AccountImported(
  id: Int
)

object AccountImported {
  implicit val Codec: JsonValueCodec[AccountImported] = JsonCodecMaker.make
}

case class AccountUpdate(
  username: String
)

object AccountUpdate {
  implicit val Codec: JsonValueCodec[AccountUpdate] = JsonCodecMaker.make
}

case class ServerResponseError(
  field: String,
  message: String,
)
object ServerResponseError {
  implicit val Codec: JsonValueCodec[ServerResponseError] = JsonCodecMaker.make
}

case class ServerResponse[T](
  result: T,
  errors: Seq[ServerResponseError],
)
object ServerResponse {
  @nowarn("msg=JsonValueCodec\\[T\\] in method Codec is never used")
  implicit def Codec[T: JsonValueCodec]: JsonValueCodec[ServerResponse[T]] = JsonCodecMaker.make
}
