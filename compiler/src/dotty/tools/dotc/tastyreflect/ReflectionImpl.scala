package dotty.tools.dotc.tastyreflect

import dotty.tools.dotc.core._

class ReflectionImpl(val rootContext: Contexts.Context)
    extends scala.tasty.Reflection
    with CoreImpl
    with CaseDefOpsImpl
    with ConstantOpsImpl
    with ContextOpsImpl
    with FlagsOpsImpl
    with IdOpsImpl
    with ImportSelectorOpsImpl
    with QuotedOpsImpl
    with PatternOpsImpl
    with PositionOpsImpl
    with PrintersImpl
    with SettingsOpsImpl
    with SignatureOpsImpl
    with StandardDefinitions
    with SymbolOpsImpl
    with TreeOpsImpl
    with TypeOrBoundsTreesOpsImpl
    with TypeOrBoundsOpsImpl
