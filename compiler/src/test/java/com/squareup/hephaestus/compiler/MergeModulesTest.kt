package com.squareup.hephaestus.compiler

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import org.junit.Test

class MergeModulesTest {

  @Test fun `Dagger modules are empty without arguments`() {
    compile(
        """
        package com.squareup.test
        
        import com.squareup.hephaestus.annotations.compat.MergeModules
        
        @MergeModules(Any::class)
        class DaggerModule1
    """
    ) {
      assertThat(daggerModule1.daggerModule.includes).isEmpty()
      assertThat(daggerModule1.daggerModule.subcomponents).isEmpty()
    }
  }

  @Test fun `included modules are added in the composite module`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules

        @MergeModules(
            scope = Any::class,
            includes = [
              Boolean::class,
              Int::class
            ]
        )
        class DaggerModule1
    """
    ) {
      assertThat(daggerModule1.daggerModule.includes.toList())
          .containsExactly(Boolean::class, Int::class)
    }
  }

  @Test fun `includes and subcomponents are added in the Dagger module`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules

        @MergeModules(
            scope = Any::class,
            includes = [
              Boolean::class,
              Int::class
            ],
            subcomponents = [
              Boolean::class,
              Int::class
            ]
        )
        class DaggerModule1
    """
    ) {
      val module = daggerModule1.daggerModule
      assertThat(module.includes.toList()).containsExactly(Boolean::class, Int::class)
      assertThat(module.subcomponents.toList()).containsExactly(Boolean::class, Int::class)
    }
  }

  @Test fun `it's not allowed to have @Module and @MergeModules annotation at the same time`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules

        @MergeModules(Any::class)
        @dagger.Module
        class DaggerModule1
    """
    ) {
      assertThat(exitCode).isEqualTo(COMPILATION_ERROR)
      // Position to the class.
      assertThat(messages).contains("Source.kt: (7, 7)")
    }
  }

  @Test fun `modules are merged`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules
        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        @dagger.Module
        abstract class DaggerModule2

        @MergeModules(Any::class)
        class DaggerModule1
    """
    ) {
      assertThat(daggerModule1.daggerModule.includes.toList()).containsExactly(daggerModule2.kotlin)
    }
  }

  @Test fun `modules are merged with included modules`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules
        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        @dagger.Module
        abstract class DaggerModule2

        @MergeModules(
            scope = Any::class,
            includes = [
              Boolean::class,
              Int::class
            ]
        )
        class DaggerModule1
    """
    ) {
      assertThat(daggerModule1.daggerModule.includes.toList())
          .containsExactly(daggerModule2.kotlin, Int::class, Boolean::class)
    }
  }

  @Test fun `contributing module must be a Dagger Module`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.ContributesTo
        import com.squareup.hephaestus.annotations.compat.MergeModules

        @ContributesTo(Any::class)
        abstract class DaggerModule1

        @MergeModules(Any::class)
        class DaggerModule1
    """
    ) {
      assertThat(exitCode).isEqualTo(COMPILATION_ERROR)
      // Position to the class.
      assertThat(messages).contains("Source.kt: (7, 16)")
    }
  }

  @Test fun `module can be replaced`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules
        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        @dagger.Module
        abstract class DaggerModule2

        @ContributesTo(
            Any::class,
            replaces = DaggerModule2::class
        )
        @dagger.Module
        abstract class DaggerModule3

        @MergeModules(Any::class)
        class DaggerModule1
    """
    ) {
      assertThat(daggerModule1.daggerModule.includes.toList()).containsExactly(daggerModule3.kotlin)
    }
  }

  @Test fun `replaced modules must be Dagger modules`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules
        import com.squareup.hephaestus.annotations.ContributesTo

        abstract class DaggerModule3

        @ContributesTo(
            Any::class,
            replaces = DaggerModule3::class
        )
        @dagger.Module
        abstract class DaggerModule2

        @MergeModules(Any::class)
        class DaggerModule1
    """
    ) {
      assertThat(exitCode).isEqualTo(COMPILATION_ERROR)
      // Position to the class.
      assertThat(messages).contains("Source.kt: (13, 16)")
    }
  }

  @Test fun `included modules are not replaced`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules
        import com.squareup.hephaestus.annotations.ContributesTo

        @dagger.Module
        abstract class DaggerModule2

        @ContributesTo(
            Any::class,
            replaces = DaggerModule2::class
        )
        @dagger.Module
        abstract class DaggerModule3

        @MergeModules(
            scope = Any::class,
            includes = [
              DaggerModule2::class
            ]
        )
        class DaggerModule1
    """
    ) {
      assertThat(daggerModule1.daggerModule.includes.toList())
          .containsExactly(daggerModule2.kotlin, daggerModule3.kotlin)
    }
  }

  @Test fun `modules can be excluded`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules
        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        @dagger.Module
        abstract class DaggerModule2

        @ContributesTo(Any::class)
        @dagger.Module
        abstract class DaggerModule3

        @MergeModules(
            scope = Any::class,
            exclude = [
              DaggerModule2::class
            ]
        )
        class DaggerModule1
    """
    ) {
      assertThat(daggerModule1.daggerModule.includes.toList()).containsExactly(daggerModule3.kotlin)
    }
  }

  @Test fun `included modules are not excluded`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules
        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        @dagger.Module
        abstract class DaggerModule2

        @ContributesTo(Any::class)
        @dagger.Module
        abstract class DaggerModule3

        @MergeModules(
            scope = Any::class,
            includes = [
              DaggerModule2::class
            ],
            exclude = [
              DaggerModule2::class,
              DaggerModule3::class
            ]
        )
        class DaggerModule1
    """
    ) {
      assertThat(daggerModule1.daggerModule.includes.toList()).containsExactly(daggerModule2.kotlin)
    }
  }

  @Test fun `modules are added to merged modules with corresponding scope`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules
        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        @dagger.Module
        abstract class DaggerModule3

        @ContributesTo(Unit::class)
        @dagger.Module
        abstract class DaggerModule4

        @MergeModules(Any::class)
        class DaggerModule1

        @MergeModules(Unit::class)
        class DaggerModule2
    """
    ) {
      assertThat(daggerModule1.daggerModule.includes.toList())
          .containsExactly(daggerModule3.kotlin)
      assertThat(daggerModule2.daggerModule.includes.toList())
          .containsExactly(daggerModule4.kotlin)
    }
  }

  @Test fun `contributed modules must be public`() {
    val visibilities = setOf(
        "internal", "private", "protected"
    )

    visibilities.forEach { visibility ->
      compile(
          """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules
        import com.squareup.hephaestus.annotations.ContributesTo

        @ContributesTo(Any::class)
        @dagger.Module
        $visibility abstract class DaggerModule2

        @MergeModules(Any::class)
        class DaggerModule1
    """
      ) {
        assertThat(exitCode).isEqualTo(COMPILATION_ERROR)
        // Position to the class.
        assertThat(messages).contains("Source.kt: (8, ")
      }
    }
  }

  @Test fun `inner modules are merged`() {
    compile(
        """
        package com.squareup.test

        import com.squareup.hephaestus.annotations.compat.MergeModules
        import com.squareup.hephaestus.annotations.ContributesTo

        @MergeModules(Any::class)
        class DaggerModule1 {
          @ContributesTo(Any::class)
          @dagger.Module
          abstract class InnerModule
        }
    """
    ) {
      val innerModule = classLoader.loadClass("com.squareup.test.DaggerModule1\$InnerModule")

      assertThat(daggerModule1.daggerModule.includes.toList())
          .containsExactly(innerModule.kotlin)
    }
  }
}
