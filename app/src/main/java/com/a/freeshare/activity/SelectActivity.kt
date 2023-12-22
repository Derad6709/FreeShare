package com.a.freeshare.activity

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.a.freeshare.R
import com.a.freeshare.fragment.*
import com.a.freeshare.fragment.media.*
import com.a.freeshare.fragment.search.SearchFragment
import com.a.freeshare.fragment.search.SearchResultFragmentList
import com.a.freeshare.impl.CommonSelectionImpl
import com.a.freeshare.obj.FileItem
import com.a.freeshare.util.FileUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.internal.EdgeToEdgeUtils

class SelectActivity: AppCompatActivity() {

    private lateinit var bottomNav : BottomNavigationView

    private  var photosFragment: PhotosFragmentList? = null
    private var videosFragment: VideosFragmentList? = null
    private  var songsFragment: SongsFragmentList? = null
    private var filesFragment: FileTreeFragmentList?= null
    private  var appsFragment: AppsFragmentList? = null

    private lateinit var latestFragment:MediaListCommonFragment

    private lateinit var toolbar: MaterialToolbar
    private lateinit var toolbarCurrentTitle:String
    private var nonZero = false

    private var isLatestFragmentGrid = false

    private lateinit var insets:androidx.core.graphics.Insets

    companion object{
        private const val TAG = "SelectActivity"
        private const val LATEST_FRAGMENT_TAG = "latestFragmentTag"
        const val ACTION_SELECT = "SelectActivity_ACTION_SELECT"
        const val TOOLBAR_STATE_STRING = "toolbar_state_string"
        const val NON_ZERO = "non_zero"

        const val CURRENT_FRAG_SORT_ID ="current_sort"
    }

    private val backPressedCallback = object : OnBackPressedCallback(true){

        override fun handleOnBackPressed() {

            supportFragmentManager.apply {

                if (supportFragmentManager.findFragmentByTag(SearchFragment.TAG) != null && supportFragmentManager.findFragmentByTag(SearchFragment.TAG)!!.isVisible){

                    /*var sel:HashMap<Long,Int>? = null
                    supportFragmentManager.findFragmentByTag(SearchResultFragmentList.TAG)?.also {
                        if (it is SearchResultFragmentList){
                            sel = it.dAdapter.getSelection()
                        }
                    }

                    if (sel != null)latestFragment.dAdapter.setSelection(sel!!)*/
                    
                    setSelectionStates()

                    ValueAnimator.ofFloat(0f,1f).apply {

                        bottomNav.visibility = View.VISIBLE

                        duration = 700
                        addUpdateListener {
                            bottomNav.alpha = it.animatedValue as Float
                        }
                        start()

                        doOnEnd {
                            val view = findViewById<FrameLayout>(R.id.activity_select_fragment_holder)
                            val l = view.layoutParams as MarginLayoutParams
                            l.bottomMargin = 0
                            view.layoutParams= l
                            view.requestLayout()

                        }
                    }

                    supportFragmentManager.findFragmentByTag(SearchFragment.TAG).also {f1->

                        supportFragmentManager.findFragmentByTag(SearchResultFragmentList.TAG).also { f2->

                            if (f1 != null && f2 != null){
                                commit {
                                    hide(f1)
                                    hide(f2)
                                    show(latestFragment)
                                }
                            }

                        }
                    }

                }else if (latestFragment.hasCleared()){
                   finish()
                }
            }


        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        EdgeToEdgeUtils.applyEdgeToEdge(window,true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select)

        initializeFragments(savedInstanceState)

        onBackPressedDispatcher.addCallback(this,backPressedCallback)

        toolbarCurrentTitle = savedInstanceState?.getString(TOOLBAR_STATE_STRING)
            ?: getString(R.string.select_items)
        nonZero = savedInstanceState?.getBoolean(NON_ZERO) ?: false

        toolbar = findViewById<MaterialToolbar>(R.id.toolbar).also{

            setSupportActionBar(it)
            it.setNavigationOnClickListener { backPressedCallback.handleOnBackPressed() }
        }

        toolbar.title = toolbarCurrentTitle

        bottomNav = findViewById(R.id.activity_select_bottom_nav)

        bottomNav.setOnItemSelectedListener{

            supportFragmentManager.commit {
                hide(latestFragment)
            }

            when(it.itemId){
                R.id.activity_select_bottom_nav_photos->{

                    if (supportFragmentManager.findFragmentByTag(PhotosFragmentList.TAG) == null){
                        supportFragmentManager.commit {
                           photosFragment =  PhotosFragmentList().also {
                                add(R.id.activity_select_fragment_holder,it,PhotosFragmentList.TAG).hide(it)
                            }
                        }

                    }

                    latestFragment = photosFragment!!
                }

                R.id.activity_select_bottom_nav_videos->{
                    if (supportFragmentManager.findFragmentByTag(VideosFragmentList.TAG) == null){
                        supportFragmentManager.commit {
                            videosFragment =  VideosFragmentList().also {
                                add(R.id.activity_select_fragment_holder,it,VideosFragmentList.TAG).hide(it)
                            }
                        }


                    }

                    latestFragment = videosFragment!!
                }

                R.id.activity_select_bottom_nav_songs->{
                    if (supportFragmentManager.findFragmentByTag(SongsFragmentList.TAG) == null){
                        supportFragmentManager.commit {
                            songsFragment =  SongsFragmentList().also {
                                add(R.id.activity_select_fragment_holder,it,SongsFragmentList.TAG).hide(it)
                            }
                        }


                    }

                    latestFragment = songsFragment!!
                }

                R.id.activity_select_bottom_nav_files->{
                    if (supportFragmentManager.findFragmentByTag(FileTreeFragmentList.TAG) == null){
                        supportFragmentManager.commit {
                            filesFragment =  FileTreeFragmentList().also { ff->
                                ff.arguments = Bundle().apply {
                                    putString(FileTreeFragmentList.SRC_DIR_PATH,Environment.getExternalStorageDirectory().absolutePath)
                                }
                                add(R.id.activity_select_fragment_holder,ff,FileTreeFragmentList.TAG).hide(ff)
                            }
                        }


                    }

                    latestFragment = filesFragment!!
                }

                R.id.activity_select_bottom_nav_apps->{
                    if (supportFragmentManager.findFragmentByTag(AppsFragmentList.TAG) == null){
                        supportFragmentManager.commit {
                            appsFragment =  AppsFragmentList().also {
                                add(R.id.activity_select_fragment_holder,it,AppsFragmentList.TAG).hide(it)
                            }
                        }


                    }

                    latestFragment = appsFragment!!
                }

            }

            supportFragmentManager.commit {
                show(latestFragment)
            }

            return@setOnItemSelectedListener true
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sfc)) { v, insets ->

            val i = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val l = v.layoutParams as MarginLayoutParams
            l.topMargin = i.top
            l.leftMargin = i.left
            l.rightMargin = i.right
            l.bottomMargin = i.bottom

            WindowInsetsCompat.CONSUMED
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_select_fragment_holder)){v,insets ->

            this@SelectActivity.insets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            return@setOnApplyWindowInsetsListener WindowInsetsCompat.CONSUMED
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {

        isLatestFragmentGrid = if (latestFragment is CommonSelectionImpl<*>)(latestFragment as CommonSelectionImpl<*>).isGridLayout() else false

        if (latestFragment is BaseFragment)
        (menu?.findItem(R.id.sort_by))?.subMenu?.findItem(latestFragment.getSortTypeId()!!)?.isChecked = true

        menu?.findItem(R.id.layout_type)?.isChecked = isLatestFragmentGrid

        return super.onPrepareOptionsMenu(menu)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        if (menu is MenuBuilder)(menu ).setOptionalIconsVisible(true)

        menuInflater.inflate(R.menu.select_activity_appbar_menu,menu)

        menu?.findItem(R.id.send)?.isVisible = nonZero

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){

            R.id.search->{

                findViewById<View>(R.id.activity_select_fragment_holder).apply {

                    val l= layoutParams as MarginLayoutParams
                    l.bottomMargin = insets.bottom
                    requestLayout()
                }

                ValueAnimator.ofFloat(1f,0f).apply {

                    bottomNav.visibility = View.GONE

                    duration = 700
                    addUpdateListener {
                        bottomNav.alpha = it.animatedValue as Float
                    }
                    start()

                }

                supportFragmentManager.commit {
                    setCustomAnimations(R.anim.slide_in_top,R.anim.fade_out,R.anim.fade_in,R.anim.slide_out_top)

                    setReorderingAllowed(true)

                    hide(latestFragment)

                    supportFragmentManager.findFragmentByTag(SearchFragment.TAG).also {f1->

                        supportFragmentManager.findFragmentByTag(SearchResultFragmentList.TAG).also { f2->

                            if ( f1 == null || f2 == null){
                                add(R.id.sfc,SearchFragment(),SearchFragment.TAG)

                                val sf = SearchResultFragmentList()
                                sf.arguments = Bundle().also {arg->
                                    arg.putSerializable(BaseFragment.SELECTION_HASH_MAP,latestFragment.getSelectionHashMap())
                                }

                                add(R.id.activity_select_fragment_holder,sf,SearchResultFragmentList.TAG)
                            }else{
                                show(f1)
                                show(f2)
                            }

                        }
                    }

                }

            }

            R.id.layout_type->{

                if (latestFragment != null && latestFragment is CommonSelectionImpl<*>){
                    (latestFragment as CommonSelectionImpl<*>).toggleGridLayout()
                }

                item.isChecked = !item.isChecked
            }

            R.id.send->{

                handleSendActionWithIntent()
            }

            R.id.sort_by_az,R.id.sort_by_za,
            R.id.sort_by_new,R.id.sort_by_old,
            R.id.sort_by_large,R.id.sort_by_small->{
               if (latestFragment is CommonSelectionImpl<*>) (latestFragment as CommonSelectionImpl<*>).sort(item.itemId)
                item.isChecked = true
            }

            R.id.clear_selection->{
                if (latestFragment != null && latestFragment is CommonSelectionImpl<*>){
                    (latestFragment as CommonSelectionImpl<*>).clearSelection()

                    setSelectionStates()
                }

            }

            R.id.select_inverse->{
                if (latestFragment != null && latestFragment is CommonSelectionImpl<*>){
                    (latestFragment as CommonSelectionImpl<*>).selectInverse()

                    setSelectionStates()
                }

            }

            R.id.select_all->{
                if (latestFragment != null && latestFragment is CommonSelectionImpl<*>){
                    (latestFragment as CommonSelectionImpl<*>).selectAll()
                    setSelectionStates()
                }


            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(LATEST_FRAGMENT_TAG,latestFragment.tag)
        outState.putString(TOOLBAR_STATE_STRING,toolbar.title.toString())
        outState.putBoolean(NON_ZERO,toolbar.menu.findItem(R.id.send).isVisible)
        outState.putInt(CURRENT_FRAG_SORT_ID,latestFragment.getSortTypeId()!!)
    }

    @SuppressLint("RestrictedApi")
    fun setSelectionStates(){

        var totalBytes:Long = 0L
        var count :Int = 0
        for (fragment in supportFragmentManager.fragments){
            if (fragment is CommonSelectionImpl<*> && fragment !is SearchResultFragmentList){
                for (i in fragment.getSelectedItems() as ArrayList<FileItem>){
                    totalBytes+= i.dataSize
                    count++
                }
            }
        }


        toolbar.title = "${getString(R.string.total)} ${getString(R.string.middle_dot)} $count ${getString(R.string.middle_dot)} ${FileUtil.getFormattedLongData(totalBytes)}"

        if (count ==0){
            toolbar.title = getString(R.string.select_items)

            toolbar.menu.findItem(R.id.send).isVisible = false
        }else{
            toolbar.menu.findItem(R.id.send).isVisible = true
        }

    }

    private fun initializeFragments(savedInstanceState: Bundle?) {

       if (savedInstanceState == null){
            supportFragmentManager.commit {
                photosFragment = PhotosFragmentList().also {
                    add(R.id.activity_select_fragment_holder,it,PhotosFragmentList.TAG).hide(it)
                }
            }

           latestFragment = photosFragment!!
       }else{

           supportFragmentManager.findFragmentByTag(PhotosFragmentList.TAG)?.also {
               photosFragment =  it as PhotosFragmentList
           }
           supportFragmentManager.findFragmentByTag(VideosFragmentList.TAG)?.also {
               videosFragment =it as VideosFragmentList
           }
           supportFragmentManager.findFragmentByTag(SongsFragmentList.TAG)?.also {
               songsFragment =  it  as SongsFragmentList
           }
           supportFragmentManager.findFragmentByTag(FileTreeFragmentList.TAG)?.also {
               filesFragment = it as FileTreeFragmentList
           }
           supportFragmentManager.findFragmentByTag(AppsFragmentList.TAG)?.also {
               appsFragment = it as AppsFragmentList
           }

           latestFragment = supportFragmentManager.findFragmentByTag(savedInstanceState.getString(
               LATEST_FRAGMENT_TAG)) as MediaListCommonFragment
       }

        supportFragmentManager.commit {
            show(latestFragment)

        }

    }

    private fun handleSendActionWithIntent(){

        val arr = ArrayList<FileItem>()

        for (fragment in supportFragmentManager.fragments){

            if (fragment is CommonSelectionImpl<*>){
                arr.addAll(fragment.getSelectedItems() as ArrayList<FileItem>)

            }
        }

        if (intent.action == ACTION_SELECT ){

            val result = Intent()
            result.putExtra(BaseFragment.ITEMS,arr)
            setResult(RESULT_OK,result)
            finish()
        }else {


            val i = Intent(this@SelectActivity,SessionActivity::class.java)
            i.putExtra(BaseFragment.ITEMS,arr)
            i.putExtra(SessionActivity.SESSION_TYPE,SessionActivity.SESSION_TYPE_SEND)
            startActivity(i)

        }
    }

    fun getLatestFragment():BaseFragment{
        return latestFragment
    }
}
