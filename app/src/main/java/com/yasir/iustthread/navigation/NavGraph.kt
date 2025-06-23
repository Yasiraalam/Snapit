package com.yasir.iustthread.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yasir.iustthread.R
import com.yasir.iustthread.internetConnectiviy.ConnectivityObserver
import com.yasir.iustthread.internetConnectiviy.NetworkConnectivityObserver
import com.yasir.iustthread.presentation.addpost.composable.AddThreads
import com.yasir.iustthread.presentation.home.composable.BottomNav
import com.yasir.iustthread.presentation.home.composable.Home
import com.yasir.iustthread.presentation.login.composable.Login
import com.yasir.iustthread.presentation.home.composable.Notification
import com.yasir.iustthread.presentation.home.composable.OtherUsers
import com.yasir.iustthread.presentation.profile.composable.Profile
import com.yasir.iustthread.presentation.profile.composable.Register
import com.yasir.iustthread.presentation.home.composable.Search
import com.yasir.iustthread.presentation.home.composable.Splash

@Composable
fun NavGraph(
    navController: NavHostController,
    connectivityObserver: NetworkConnectivityObserver
) {
    val status by connectivityObserver.observe().collectAsState(
        initial = ConnectivityObserver.Status.Available
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (status == ConnectivityObserver.Status.Available) {
            NavHost(
                navController = navController,
                startDestination = Routes.Splash.routes
            ){
                composable(Routes.Splash.routes){
                    Splash(navController)

                }

                composable(Routes.Home.routes){
                    Home(navController)

                }

                composable(Routes.Notification.routes){
                    Notification()
                }

                composable(Routes.Search.routes){
                    Search(navController)
                }

                composable(Routes.AddThread.routes){
                    AddThreads(navController)
                }

                composable(Routes.Profile.routes){
                    Profile(navController)
                }
                composable(Routes.BottomNav.routes){
                    BottomNav(navController)
                }
                composable(Routes.Login.routes){
                    Login(navController)
                }
                composable(Routes.Register.routes){
                    Register(navController)
                }
                composable(Routes.OtherUsers.routes){
                    val data = it.arguments!!.getString("data")
                    OtherUsers(navController,data!!)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier.size(300.dp),
                    painter = painterResource(id = R.drawable.no_wifi),
                    contentDescription ="",
                    )
                Text(
                    modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp),
                    text = "No Internet Connection",
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp),
                    text = "Unable to establish a connection to the internet. Please check your network settings",
                    color = Color.Black,
                )
            }
        }
    }


}