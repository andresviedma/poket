package com.github.andresviedma.poket.monitoring

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier

fun <R : Any> R.logger(): Lazy<Logger> =
    lazy { LoggerFactory.getLogger(unwrapCompanionClass(this.javaClass).name) }

/** unwrap companion class to enclosing class given a Java Class */
private fun <T : Any> unwrapCompanionClass(clazz: Class<T>): Class<*> =
    clazz.enclosingClass?.let { enclosingClass ->
        try {
            enclosingClass.declaredFields
                .find { field ->
                    field.name == clazz.simpleName &&
                            Modifier.isStatic(field.modifiers) &&
                            field.type == clazz
                }
                ?.run { enclosingClass }
        } catch (se: SecurityException) {
            // The security manager isn't properly set up, so it won't be possible
            // to search for the target declared field.
            null
        }
    } ?: clazz
