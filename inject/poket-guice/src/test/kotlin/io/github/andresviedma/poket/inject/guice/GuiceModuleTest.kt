package io.github.andresviedma.poket.inject.guice

import com.google.inject.Injector
import io.github.andresviedma.poket.inject.testcommons.InjectionModuleBaseTest

class GuiceModuleTest : InjectionModuleBaseTest<Injector>(GuiceInjector())
