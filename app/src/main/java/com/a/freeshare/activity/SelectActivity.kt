package com.a.freeshare.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.commit
import com.a.freeshare.R
import com.a.freeshare.fragment.*
import com.a.freeshare.fragment.media.*
import com.a.freeshare.impl.CommonSelectionImpl
import com.a.freeshare.obj.FileItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class SelectActivity: AppCompatActivity() {

    private lateinit var bottomNav : BottomNavigationView

    private lateinit var photosFragment: PhotosFragment
    private lateinit var videosFragment: VideosFragment
    private lateinit var songsFragment: SongsFragment
    private lateinit var filesFragment: FileTreeFragment
    private lateinit var appsFragment: AppsFragment

    private lateinit var latestFragment:BaseFragment

    companion object{
        private const val LATEST_FRAGMENT_TAG = "latestFragmentTag"
    }

    private val backPressedCallback = object : OnBackPressedCallback(true){

        override fun handleOnBackPressed() {
           if (latestFragment.hasCleared())finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)

        initializeFragments(savedInstanceState)

        onBackPressedDispatcher.addCallback(this,backPressedCallback)

        findViewById<MaterialToolbar>(R.id.toolbar).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { backPressedCallback.handleOnBackPressed() }
        }

        bottomNav = findViewById(R.id.activity_select_bottom_nav)

        bottomNav.setOnItemSelectedListener{

            val prevFragment = latestFragment

            when(it.itemId){
                R.id.activity_select_bottom_nav_photos->{

                    latestFragment = photosFragment
                }

                R.id.activity_select_bottom_nav_videos->{
                   latestFragment = videosFragment
                }

                R.id.activity_select_bottom_nav_songs->{
                   latestFragment = songsFragment
                }

                R.id.activity_select_bottom_nav_files->{
                   latestFragment = filesFragment
                }

                R.id.activity_select_bottom_nav_apps->{
                   latestFragment = appsFragment
                }

            }

            supportFragmentManager.commit {
                hide(prevFragment)
                show(latestFragment)
            }

            return@setOnItemSelectedListener true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.select_activity_appbar_menu,menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){

            R.id.send->{

                val arr = ArrayList<FileItem>()

                for (fragment in supportFragmentManager.fragments){



                  if (fragment is CommonSelectionImpl<*>){
                      arr.addAll(fragment.getSelectedItems() as ArrayList<FileItem>)
                  }
                }

                val i = Intent(this,SessionActivity::class.java)
                i.putExtra(BaseFragment.ITEMS,arr)
                i.putExtra(SessionActivity.SESSION_TYPE,SessionActivity.SESSION_TYPE_SEND)
                startActivity(i)
            }

            R.id.sort_by_az,R.id.sort_by_za,
            R.id.sort_by_new,R.id.sort_by_old,
            R.id.sort_by_large,R.id.sort_by_small->{
               if(latestFragment is PhotosFragment)(latestFragment as PhotosFragment).sort(item.itemId)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(LATEST_FRAGMENT_TAG,latestFragment.tag)
    }

    private fun initializeFragments(savedInstanceState: Bundle?) {
       if (savedInstanceState == null){
           photosFragment = PhotosFragment()
           videosFragment = VideosFragment()
           songsFragment = SongsFragment()
           filesFragment = FileTreeFragment().apply {
               arguments = Bundle().apply {
                   putString(FileTreeFragment.SRC_DIR_PATH,Environment.getExternalStorageDirectory().absolutePath)
               }
           }
           appsFragment = AppsFragment()

           supportFragmentManager.commit {
               add(R.id.activity_select_fragment_holder,photosFragment, PhotosFragment.TAG).hide(photosFragment)
               add(R.id.activity_select_fragment_holder,videosFragment, VideosFragment.TAG).hide(videosFragment)
               add(R.id.activity_select_fragment_holder,songsFragment, SongsFragment.TAG).hide(songsFragment)
               add(R.id.activity_select_fragment_holder,filesFragment, FileTreeFragment.TAG).hide(filesFragment)
               add(R.id.activity_select_fragment_holder,appsFragment, AppsFragment.TAG).hide(appsFragment)
           }

           latestFragment = photosFragment
       }else{
           photosFragment = supportFragmentManager.findFragmentByTag(PhotosFragment.TAG) as PhotosFragment
           videosFragment = supportFragmentManager.findFragmentByTag(VideosFragment.TAG) as VideosFragment
           songsFragment = supportFragmentManager.findFragmentByTag(SongsFragment.TAG) as SongsFragment
           filesFragment = supportFragmentManager.findFragmentByTag(FileTreeFragment.TAG) as FileTreeFragment
           appsFragment = supportFragmentManager.findFragmentByTag(AppsFragment.TAG) as AppsFragment

           latestFragment = supportFragmentManager.findFragmentByTag(savedInstanceState.getString(
               LATEST_FRAGMENT_TAG)) as BaseFragment
       }

        supportFragmentManager.commit {
            show(latestFragment)
        }
    }

}