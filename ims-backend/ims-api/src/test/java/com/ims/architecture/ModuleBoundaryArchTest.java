package com.ims.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.ims", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryArchTest {

  @ArchTest
  static final ArchRule financeMustNotDependOnPeoplePersistence =
      noClasses()
          .that()
          .resideInAPackage("com.ims.finance..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.ims.people..infrastructure..")
          .because("modules must not reach into another module's persistence");

  @ArchTest
  static final ArchRule academicMustNotDependOnPeoplePersistence =
      noClasses()
          .that()
          .resideInAPackage("com.ims.academic..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.ims.people..infrastructure..")
          .because("academic must use FacultyService, not faculty persistence");

  @ArchTest
  static final ArchRule academicMustNotDependOnFinancePersistence =
      noClasses()
          .that()
          .resideInAPackage("com.ims.academic..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.ims.finance..infrastructure..")
          .because("modules must not reach into another module's persistence");

  @ArchTest
  static final ArchRule peopleMustNotDependOnFinancePersistence =
      noClasses()
          .that()
          .resideInAPackage("com.ims.people..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.ims.finance..infrastructure..");

  @ArchTest
  static final ArchRule operationsMustNotDependOnAcademicPersistence =
      noClasses()
          .that()
          .resideInAPackage("com.ims.operations..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.ims.academic..infrastructure..")
          .because("operations must use academic application services");

  @ArchTest
  static final ArchRule operationsMustNotDependOnPeoplePersistence =
      noClasses()
          .that()
          .resideInAPackage("com.ims.operations..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.ims.people..infrastructure..")
          .because("operations must use people application services");

  @ArchTest
  static final ArchRule documentsMustNotDependOnPeoplePersistence =
      noClasses()
          .that()
          .resideInAPackage("com.ims.documents..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.ims.people..infrastructure..")
          .because("documents must use people application services");

  @ArchTest
  static final ArchRule documentsMustNotDependOnAcademicPersistence =
      noClasses()
          .that()
          .resideInAPackage("com.ims.documents..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.ims.academic..infrastructure..")
          .because("documents must use academic application services");

  @ArchTest
  static final ArchRule importsMustNotDependOnPeoplePersistence =
      noClasses()
          .that()
          .resideInAPackage("com.ims.imports..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.ims.people..infrastructure..")
          .because("imports must use people application services");
}
