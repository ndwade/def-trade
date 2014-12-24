/*

Issues:

need a way to identify arbitrary messages to be transformed,
so sender can match Ack

there needs to be an init phase where the only messages responded to
are init messages: example: NextValidOrderId, other messages are ignored

THIS IS ALL EFFECTIVELY JUST PSEUDOCODE: need to "eventize" all this
for event sourced persistence. :|

 *
 */

class OrderService() extends Actor with Logging {

  type Xform = PartialFunction[Any, Order]

  import OrderService._

  // all these vars need to be Persisted
  var nextId: Option[Int] = None
  def nextOrderId() = {
    val ret = nextId.get // (sic: actually should log; major error)
    nextOrderId map (_ + 1)
    ret
  }
  var xforms: Map[ActorRef, PartialFunction[Any, Order]]
  var orders: Map[OrderId, OrderRecord] = ???

  def processOrder(_order: Order): Unit = {
    val order = _order.copy(id = nextOrderId())
    val riskStatus = riskCheck(order)
    if (riskCheck.isOk) {
      conn ! order
    }
    sender ! Ack(order, riskCheck)
  }


  // presumes ready after receiving nextId
  override def receive: Receive = {
    case NextValidOrderId(id) =>
      nextId = Some(id.id)
      become trade
  }
  def trade: Receive = {  // niiice

    case NextValidOrderId(id) => nextId = id

    case AddXform(x) =>
      val curr = xforms getOrElse (sender, PartialFunction.empty)
      xforms += sender -> (curr orElse x)

    case msg: OrderMsg =>
      val order = xforms(sender)(msg)
      processOrder(order)

    case os: OrderStatus => ???
    case oo: OpenOrder => ???
    case ooe: OpenOrderEnd => ???
    case xd: ExecutionDetails => ???
    case xde: ExecutionDetailsEnd => ???
  }
}

object OrderService {

  trait OrderMsg {
    def msgId: MsgId // for idempotency: reliable messaging pattern
    def stratId: StratId // same strategy could use different trader actors
  }
  case class AddXform(x: Xform)
  case class Ack(o: Order, rc: RiskCheck)
}
