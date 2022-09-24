/*
 * Copyright (c) 2020 Nam Nguyen, nam@ene.im
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kohii.v1.sample.tiktok

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kohii.v1.sample.tiktok.databinding.ActivityMainBinding
import kohii.v1.sample.tiktok.ui.dashboard.DashboardFragment
import kohii.v1.sample.tiktok.ui.home.HomeFragment
import kohii.v1.sample.tiktok.ui.notifications.NotificationsFragment

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Reuse Fragments instead of creating new to keep the last visible video position in list.
    // This is not a good practice, since it consume more memory.
    // Take a look here for a better way: [NavigationAdvancedSample](https://github.com/android/architecture-components-samples/blob/master/NavigationAdvancedSample/app/src/main/java/com/example/android/navigationadvancedsample/NavigationExtensions.kt)
    val fragments = hashMapOf(
        DashboardFragment::class.java.name to DashboardFragment(),
        HomeFragment::class.java.name to HomeFragment(),
        NotificationsFragment::class.java.name to NotificationsFragment()
    )

    // Must call before setContentView.
    val defaultFactory = supportFragmentManager.fragmentFactory
    supportFragmentManager.fragmentFactory = object : FragmentFactory() {
      override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return fragments.getOrElse(className) { defaultFactory.instantiate(classLoader, className) }
      }
    }

    val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    val navController = findNavController(R.id.nav_host_fragment)

    // Passing each menu ID as a set of Ids because each
    // menu should be considered as top level destinations.
    val appBarConfiguration = AppBarConfiguration(
        setOf(
            R.id.navigation_home,
            R.id.navigation_dashboard,
            R.id.navigation_notifications
        )
    )

    setupActionBarWithNavController(navController, appBarConfiguration)
    binding.navView.setupWithNavController(navController)
  }
}
