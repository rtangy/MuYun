plugins {
    java
    `java-library`
}

dependencies {
    api(project(":muyun-core"))
    //TODO 测试同时依赖的情况
    api(project(":muyun-database-std"))
    api(project(":muyun-authorization"))

    api(libs.easyCaptcha)
//    api(project(":muyun-database-uni"))
}
