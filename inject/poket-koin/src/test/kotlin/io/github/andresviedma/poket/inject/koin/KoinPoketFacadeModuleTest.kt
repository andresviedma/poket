package io.github.andresviedma.poket.inject.koin

import io.github.andresviedma.poket.inject.testcommons.PoketFacadeModuleBaseTest
import org.koin.core.Koin

class KoinPoketFacadeModuleTest : PoketFacadeModuleBaseTest<Koin>(KoinInjector())
