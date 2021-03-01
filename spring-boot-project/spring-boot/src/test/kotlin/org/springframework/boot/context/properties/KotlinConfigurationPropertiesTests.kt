package org.springframework.boot.context.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.support.TestPropertySourceUtils

/**
 * Tests for {@link ConfigurationProperties @ConfigurationProperties}-annotated beans.
 *
 * @author Madhura Bhave
 * @author Hyunjin Choi
 */
class KotlinConfigurationPropertiesTests {

	private var context = AnnotationConfigApplicationContext()

	@Test //gh-18652
	fun `type with constructor binding and existing singleton should not fail`() {
		val beanFactory = this.context.beanFactory
		(beanFactory as BeanDefinitionRegistry).registerBeanDefinition("foo",
				RootBeanDefinition(BingProperties::class.java))
		beanFactory.registerSingleton("foo", BingProperties(""))
		this.context.register(TestConfig::class.java)
		this.context.refresh()
	}

	@Test
	fun `map property with constructor binding should load value as map`() {
		load(ConstructorParameterConfiguration::class.java, "properties.name:name", "properties.value:value")
		val bean = this.context.getBean(MapProperties::class.java)
		assertThat(bean.properties["name"]).isEqualTo("name")
		assertThat(bean.properties["value"]).isEqualTo("value")
	}

	@Test
	fun `map property with delegation should access as map interface`() {
		load(ConstructorParameterConfiguration::class.java, "properties.name:name", "properties.value:value")
		val bean = this.context.getBean(MapPropertiesWithDelegation::class.java)
		assertThat(bean["name"]).isEqualTo("name")
		assertThat(bean["value"]).isEqualTo("value")
	}

	@Test
	fun `nested class with constructor binding should load value as nested class`() {
		load(ConstructorParameterConfiguration::class.java, "properties.name:name", "properties.value:value")
		val bean = this.context.getBean(NestedClassProperties::class.java)
		assertThat(bean.properties.name).isEqualTo("name")
		assertThat(bean.properties.value).isEqualTo("value")
	}

	@Test
	fun `nested class with delegation should load access as nested class`() {
		load(ConstructorParameterConfiguration::class.java, "properties.name:name", "properties.value:value")
		val bean = this.context.getBean(NestedClassPropertiesWithDelegation::class.java)
		assertThat(bean.name()).isEqualTo("name")
		assertThat(bean.value()).isEqualTo("value")
	}

	private fun load(configuration: Class<*>, vararg inlinedProperties: String): AnnotationConfigApplicationContext? {
		return load(arrayOf(configuration), *inlinedProperties)
	}

	private fun load(configuration: Array<Class<*>>, vararg inlinedProperties: String): AnnotationConfigApplicationContext? {
		context.register(*configuration)
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, *inlinedProperties)
		context.refresh()
		return context
	}

	@ConfigurationProperties(prefix = "foo")
	@ConstructorBinding
	class BingProperties(@Suppress("UNUSED_PARAMETER") bar: String)

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	internal open class TestConfig

	@EnableConfigurationProperties(MapPropertiesWithDelegation::class, MapProperties::class, NestedClassProperties::class, NestedClassPropertiesWithDelegation::class)
	internal class ConstructorParameterConfiguration

	@ConstructorBinding
	@ConfigurationProperties
	class MapProperties(val properties: Map<String, String>)

	@ConstructorBinding
	@ConfigurationProperties
	class MapPropertiesWithDelegation(val properties: Map<String, String>) : Map<String, String> by properties

	@ConstructorBinding
	@ConfigurationProperties
	class NestedClassProperties(val properties: Property)

	@ConstructorBinding
	@ConfigurationProperties
	class NestedClassPropertiesWithDelegation(val properties: Property) : PropertyDelegate by properties

	class Property(val name: String, val value: String) : PropertyDelegate {
		override fun name(): String {
			return this.name
		}

		override fun value(): String {
			return this.value
		}
	}

	interface PropertyDelegate {
		fun name(): String
		fun value(): String
	}
}