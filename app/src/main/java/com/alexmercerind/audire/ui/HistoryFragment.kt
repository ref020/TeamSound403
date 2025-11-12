package com.alexmercerind.audire.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.alexmercerind.audire.R
import com.alexmercerind.audire.ui.adapters.HistoryItemAdapter
import com.alexmercerind.audire.databinding.FragmentHistoryBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var imm: InputMethodManager

    private val historyViewModel: HistoryViewModel by activityViewModels()

    private val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            historyViewModel.query = MutableStateFlow(s.toString())
            historyViewModel.filterSortSearch()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        //val filterChoices: List<String> = historyViewModel.filterChoices1
        imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        _binding = FragmentHistoryBinding.inflate(inflater, container, false)

        binding.searchLinearLayout.visibility = View.GONE
        binding.historyLinearLayout.visibility = View.GONE
        binding.historyRecyclerView.adapter = HistoryItemAdapter(listOf(), historyViewModel)
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(context)

        binding.searchTextInputLayout.visibility = View.GONE

        binding.searchTextInputLayout.setEndIconOnClickListener {
            binding.searchTextInputEditText.text?.clear()
            binding.primaryMaterialToolbar.visibility = View.VISIBLE
            binding.searchTextInputLayout.visibility = View.GONE
            binding.searchTextInputLayout.clearFocus()
            imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                historyViewModel.historyItems.filterNotNull().collect {
                    if (it.isEmpty()) {
                        binding.historyRecyclerView.visibility = View.GONE
                        if (historyViewModel.query == MutableStateFlow("")) {
                            // No HistoryItem(s) by default.
                            binding.historyLinearLayout.visibility = View.VISIBLE
                            binding.searchLinearLayout.visibility = View.GONE
                        } else {
                            // No HistoryItem(s) due to search.
                            binding.historyLinearLayout.visibility = View.GONE
                            binding.searchLinearLayout.visibility = View.VISIBLE
                        }
                    } else {
                        // HistoryItem(s) are present i.e. RecyclerView must be VISIBLE.
                        binding.historyRecyclerView.visibility = View.VISIBLE
                        binding.historyLinearLayout.visibility = View.GONE
                        binding.searchLinearLayout.visibility = View.GONE

                        val adapter = binding.historyRecyclerView.adapter as HistoryItemAdapter
                        if (adapter.items.size != it.size) {
                            adapter.items = it
                            adapter.notifyDataSetChanged()
                        } else {
                            adapter.items = it
                            adapter.notifyItemRangeChanged(0, it.size)
                        }
                    }
                }
            }
        }

        val sortChoices = listOf("Artist: Ascending", "Artist: Descending", "Date Added: Ascending", "Date Added: Descending", "Title: Ascending", "Title: Descending", "Year Released: Ascending", "Year Released: Descending")
        var sortDropdownAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sortChoices)
        var filterDropdownAdapter: ArrayAdapter<String?>? = null
        var filterArtistDropdownAdapter: ArrayAdapter<String?>? = null
        var filterAlbumDropdownAdapter: ArrayAdapter<String?>? = null
        var filterYearDropdownAdapter: ArrayAdapter<String?>? = null
        lifecycleScope.launch {
            historyViewModel.getFilterArtistChoices().collect { choices ->
                println("bruh")
                filterArtistDropdownAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, choices)
            }
            historyViewModel.getFilterAlbumChoices().collect { albumChoices ->
                filterAlbumDropdownAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, albumChoices)
            }
            historyViewModel.getFilterYearChoices().collect { yearChoices ->
                println("YearADaptizzy")
                filterYearDropdownAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, yearChoices)
            }
        }
        lifecycleScope.launch {
            historyViewModel.getFilterAlbumChoices().collect { choices ->
                filterAlbumDropdownAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    choices
                )
            }
        }

        lifecycleScope.launch {
            historyViewModel.getFilterYearChoices().collect { choices ->
                filterYearDropdownAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    choices
                )
            }
        }

        val filterChoices = listOf("Clear Filters", "Artist", "Album", "Year")
        filterDropdownAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, filterChoices)
        binding.filterDropdownMenu.setAdapter(filterDropdownAdapter)
        binding.filterArtistDropdownMenu.setAdapter(filterArtistDropdownAdapter)

        binding.filterDropdownMenu.setOnClickListener {
            binding.filterDropdownMenu.setAdapter(filterDropdownAdapter)
            binding.filterDropdownMenu.showDropDown()
        }



        binding.filterDropdownMenu.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                lifecycleScope.launch {
                    historyViewModel.filters = MutableStateFlow(emptyList())
                    historyViewModel.filterSortSearch()
                }
            }
            lifecycleScope.launch {
                historyViewModel.filterType = MutableStateFlow(filterChoices[position])
            }
            println(filterChoices[position])
            when (filterChoices[position]) {
                "Clear Filters" -> {

                }
                "Artist" -> {
                    binding.filterArtistDropdownLayout.visibility = View.VISIBLE
                    binding.filterDropdownLayout.visibility = View.GONE
                    binding.filterAlbumDropdownLayout.visibility = View.GONE
                    binding.filterYearDropdownLayout.visibility = View.GONE
                    binding.filterArtistDropdownMenu.setAdapter(filterArtistDropdownAdapter)
                    binding.filterArtistDropdownMenu.showDropDown()
                }

                "Album" -> {
                    binding.filterAlbumDropdownLayout.visibility = View.VISIBLE
                    binding.filterDropdownLayout.visibility = View.GONE
                    binding.filterArtistDropdownLayout.visibility = View.GONE
                    binding.filterYearDropdownLayout.visibility = View.GONE
                    binding.filterAlbumDropdownMenu.setAdapter(filterAlbumDropdownAdapter)
                    binding.filterAlbumDropdownMenu.showDropDown()
                }

                "Year" -> {
                    println("filterYear")
                    binding.filterYearDropdownLayout.visibility = View.VISIBLE
                    binding.filterDropdownLayout.visibility = View.GONE
                    binding.filterArtistDropdownLayout.visibility = View.GONE
                    binding.filterAlbumDropdownLayout.visibility = View.GONE
                    binding.filterYearDropdownMenu.setAdapter(filterYearDropdownAdapter)
                    binding.filterYearDropdownMenu.showDropDown()
                }


            }
//            lifecycleScope.launch {
//                historyViewModel.getFilterChoices().collect { choices ->
//                    val selected = choices[position]
//
//                    if (selected == "Clear Filters") {
//                        historyViewModel.filterType = MutableStateFlow(null)
//                    } else if (selected in historyViewModel.getFilterArtistChoices().first()) {
//                        historyViewModel.filterType = MutableStateFlow("artist")
//                        historyViewModel.filterChoice = MutableStateFlow(selected)
//                    } else {
//                        historyViewModel.filterType = MutableStateFlow("year")
//                        historyViewModel.filterChoice = MutableStateFlow(selected)
//                    }
//
//                    historyViewModel.filterSortSearch()
//                }
//            }
        }

        binding.filterArtistDropdownMenu.setOnItemClickListener { _, _, position, _ ->
            // if back not selected
            println("position" + position)
            if (position != 0) {
                lifecycleScope.launch {
                    historyViewModel.getFilterArtistChoices().collect { choices ->
                        val choiceFlow: MutableStateFlow<List<String?>> =
                            MutableStateFlow(listOf("Artist", choices[position]))
                        if (choiceFlow.value in historyViewModel.filters.value) {
                            historyViewModel.filters.value = historyViewModel.filters.value.toMutableList().apply {
                                remove(choiceFlow.value)
                            }
                        } else {
                            historyViewModel.filters.value = historyViewModel.filters.value.toMutableList().apply {
                                add(choiceFlow.value)
                            }
                            println(historyViewModel.filters.value)
                        }
                        print("bruhf")
                        println(historyViewModel.filters.value)
                        var filtersString = historyViewModel.filters.value.map {it[1]}.toString()
                        filtersString = filtersString.replace("[", "")
                        filtersString = filtersString.replace("]", "")
                        historyViewModel.filterSortSearch()
                        binding.filterDropdownMenu.setText(filtersString, false)

                    }
                }
            }
            binding.filterArtistDropdownLayout.visibility = View.GONE
            binding.filterDropdownLayout.visibility = View.VISIBLE
            binding.filterDropdownMenu.showDropDown()
        }

        binding.filterAlbumDropdownMenu.setOnItemClickListener { _, _, position, _ ->
            // if back not selected
            println("position" + position)
            if (position != 0) {
                lifecycleScope.launch {
                    historyViewModel.getFilterAlbumChoices().collect { choices ->
                        val choiceFlow: MutableStateFlow<List<String?>> =
                            MutableStateFlow(listOf("Album", choices[position]))
                        if (choiceFlow.value in historyViewModel.filters.value) {
                            historyViewModel.filters.value = historyViewModel.filters.value.toMutableList().apply {
                                remove(choiceFlow.value)
                            }
                        } else {
                            historyViewModel.filters.value = historyViewModel.filters.value.toMutableList().apply {
                                add(choiceFlow.value)
                            }
                            println(historyViewModel.filters.value)
                        }
                        var filtersString = historyViewModel.filters.value.map {it[1]}.toString()
                        filtersString = filtersString.replace("[", "")
                        filtersString = filtersString.replace("]", "")
                        historyViewModel.filterSortSearch()
                        binding.filterDropdownMenu.setText(filtersString, false)

                    }
                }
            }
            binding.filterAlbumDropdownLayout.visibility = View.GONE
            binding.filterDropdownLayout.visibility = View.VISIBLE
            binding.filterDropdownMenu.showDropDown()
        }

        binding.filterYearDropdownMenu.setOnItemClickListener { _, _, position, _ ->
            // if back not selected
            if (position != 0) {
                lifecycleScope.launch {
                    historyViewModel.getFilterYearChoices().collect { choices ->
                        val choiceFlow: MutableStateFlow<List<String?>> =
                            MutableStateFlow(listOf("Year", choices[position]))
                        println(choiceFlow)
                        if (choiceFlow.value in historyViewModel.filters.value) {
                            historyViewModel.filters.value = historyViewModel.filters.value.toMutableList().apply {
                                remove(choiceFlow.value)
                            }
                        } else {
                            historyViewModel.filters.value = historyViewModel.filters.value.toMutableList().apply {
                                add(choiceFlow.value)
                            }
                        }
                        var filtersString = historyViewModel.filters.value.map {it[1]}.toString()
                        filtersString = filtersString.replace("[", "")
                        filtersString = filtersString.replace("]", "")
                        historyViewModel.filterSortSearch()
                        binding.filterDropdownMenu.setText(filtersString, false)

                    }
                }
            }
            binding.filterYearDropdownLayout.visibility = View.GONE
            binding.filterDropdownLayout.visibility = View.VISIBLE
            binding.filterDropdownMenu.showDropDown()
        }

        binding.sortDropdownMenu.setOnClickListener {
            binding.sortDropdownMenu.setAdapter(sortDropdownAdapter)
            binding.sortDropdownMenu.showDropDown()
        }

        binding.sortDropdownMenu.setOnItemClickListener { _, _, position, _ ->
            println(position)
            lifecycleScope.launch {
                val selected = sortChoices[position]
                if (position == 0 || position == 1) {
                    historyViewModel.sortChoice = MutableStateFlow("Artist")
                } else if (position == 2 || position == 3) {
                    historyViewModel.sortChoice = MutableStateFlow("Date Added")
                } else if (position == 4 || position == 5) {
                    historyViewModel.sortChoice = MutableStateFlow("Title")
                } else if (position == 6 || position == 7) {
                    historyViewModel.sortChoice = MutableStateFlow("Year")
                }
                if (position % 2 == 0) {
                    historyViewModel.isAscending = MutableStateFlow(true)
                } else {
                    historyViewModel.isAscending = MutableStateFlow(false)
                    println("noSort")
                }
                historyViewModel.filterSortSearch()
            }
        }



        binding.primaryMaterialToolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.search) {
                binding.primaryMaterialToolbar.visibility = View.GONE
                binding.searchTextInputLayout.visibility = View.VISIBLE
                binding.searchTextInputLayout.requestFocus()
                imm.showSoftInput(binding.searchTextInputEditText, 0)
            } else {
                val intent = when (it.itemId) {
                    R.id.settings -> Intent(context, SettingsActivity::class.java)
                    R.id.about -> Intent(context, AboutActivity::class.java)
                    else -> null
                }
                if (intent != null) {
                    startActivity(intent)
                }
            }
            true
        }
            return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.searchTextInputEditText.addTextChangedListener(watcher)
    }

    override fun onStop() {
        super.onStop()
        binding.searchTextInputEditText.removeTextChangedListener(watcher)
        binding.searchTextInputEditText.text?.clear()
        historyViewModel.query = MutableStateFlow("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
