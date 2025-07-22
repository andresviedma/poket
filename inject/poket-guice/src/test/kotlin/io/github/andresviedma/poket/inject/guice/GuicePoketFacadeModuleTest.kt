package io.github.andresviedma.poket.inject.guice

import com.google.inject.Injector
import io.github.andresviedma.poket.inject.testcommons.PoketFacadeModuleBaseTest

class GuicePoketFacadeModuleTest : PoketFacadeModuleBaseTest<Injector>(GuiceInjector(), injectGuiceBindings)
