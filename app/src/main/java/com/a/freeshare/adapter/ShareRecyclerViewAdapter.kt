
    class ShareRecyclerViewAdapter(private var items:ArrayList<HelperItem>):RecyclerView.Adapter<ShareRecyclerViewAdapter.BaseViewHolder>(){

        open class BaseViewHolder(private val itemView:View):AbsBaseHolder<HelperItem>(itemView){

            private val txtName:TextView = itemView.findViewById(R.id.layout_receive_title)
            private val txtSize:TextView = itemView.findViewById(R.id.layout_receive_size)
            private val imgIcon:ImageView = itemView.findViewById(R.id.layout_receive_icon)

            private lateinit var updateRunnable: Runnable

            override fun bind(a: HelperItem) {

                txtName.text = a.name
                txtSize.text = "${FileUtil.getFormattedLongData(a.currentValue)}/${FileUtil.getFormattedLongData(a.maxValue)}"

                updateRunnable = Runnable {

                    if (a.itemState == HelperItem.ItemState.ENDED){
                        txtSize.text = "${FileUtil.getFormattedLongData(a.currentValue)}/${FileUtil.getFormattedLongData(a.maxValue)}"
                        setIconOfFile(imgIcon,a.absPath,a.mime)
                        itemView.removeCallbacks(updateRunnable)
                    }else{
                        txtSize.text = "${FileUtil.getFormattedLongData(a.currentValue)}/${FileUtil.getFormattedLongData(a.maxValue)}"
                        itemView.postDelayed(updateRunnable,1000)
                    }
                }

                if (a.sharedType == HelperItem.SENT)setIconOfFile(imgIcon,a.absPath,a.mime)
                itemView.postDelayed(updateRunnable,1000)
            }
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {

            val layoutInflater = LayoutInflater.from(parent.context)

            return if (viewType == TRANSFER_VIEW_TYPE_SEND){
                BaseViewHolder(layoutInflater.inflate(R.layout.layout_send_progress,parent,false))
            }else{
                BaseViewHolder(layoutInflater.inflate(R.layout.layout_receive_progress,parent,false))
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun getItemViewType(position: Int): Int {
            return items[position].sharedType
        }

        fun addAndTrack(){

        }
    }
