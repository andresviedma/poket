package io.github.andresviedma.poket.inject.koin

import io.github.andresviedma.poket.inject.testcommons.InjectionModuleBaseTest
import org.koin.core.Koin

class KoinModuleTest : InjectionModuleBaseTest<Koin>(KoinInjector(), injectKoinBindings)
