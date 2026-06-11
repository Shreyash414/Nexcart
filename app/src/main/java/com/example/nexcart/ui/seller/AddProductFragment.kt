package com.example.nexcart.ui.seller

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nexcart.databinding.FragmentAddProductBinding
import com.example.nexcart.databinding.ItemImageSlotBinding
import com.example.nexcart.utils.UiState
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

private const val MAX_SLOTS = 5
private const val MIN_IMAGES = 3

@AndroidEntryPoint
class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddProductViewModel by viewModels()

    // ── Image URIs list (max MAX_SLOTS) ──────────────────────────────────
    private val selectedUris = mutableListOf<Uri?>().apply {
        repeat(MAX_SLOTS) { add(null) }
    }

    // Which slot is currently being filled (for the picker callback)
    private var activeSlotIndex = 0

    private var cameraImageUri: Uri? = null

    // ── Image slot adapter ────────────────────────────────────────────────
    private inner class ImageSlotAdapter : RecyclerView.Adapter<ImageSlotAdapter.SlotVH>() {

        inner class SlotVH(val b: ItemImageSlotBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemCount() = MAX_SLOTS

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotVH =
            SlotVH(ItemImageSlotBinding.inflate(layoutInflater, parent, false))

        override fun onBindViewHolder(holder: SlotVH, position: Int) {
            val uri = selectedUris[position]
            if (uri != null) {
                holder.b.ivSlotImage.isVisible = true
                holder.b.layoutSlotEmpty.isVisible = false
                holder.b.ivRemoveSlot.isVisible = true
                Glide.with(requireContext()).load(uri).centerCrop().into(holder.b.ivSlotImage)
                holder.b.ivRemoveSlot.setOnClickListener {
                    selectedUris[position] = null
                    notifyItemChanged(position)
                    updateImageCount()
                }
            } else {
                holder.b.ivSlotImage.isVisible = false
                holder.b.layoutSlotEmpty.isVisible = true
                holder.b.ivRemoveSlot.isVisible = false
            }
            holder.b.root.setOnClickListener {
                activeSlotIndex = position
                showImagePickerOptions()
            }
        }
    }

    private lateinit var slotAdapter: ImageSlotAdapter

    // ── Activity result launchers ─────────────────────────────────────────
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUris[activeSlotIndex] = it
            slotAdapter.notifyItemChanged(activeSlotIndex)
            updateImageCount()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selectedUris[activeSlotIndex] = cameraImageUri
            slotAdapter.notifyItemChanged(activeSlotIndex)
            updateImageCount()
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    // ─────────────────────────────────────────────────────────────────────

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupImageSlots()
        setupCategoryDropdown()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupImageSlots() {
        slotAdapter = ImageSlotAdapter()
        binding.rvImageSlots.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = slotAdapter
        }
        updateImageCount()
    }

    private fun updateImageCount() {
        val count = selectedUris.count { it != null }
        binding.tvImageCount.text = "$count / $MAX_SLOTS photos added"
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Electronics", "Fashion", "Home", "Beauty", "Books", "Toys")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            val title = binding.etProductName.text.toString().trim()
            val price = binding.etProductPrice.text.toString().trim()
            val category = binding.actvCategory.text.toString().trim()
            val description = binding.etProductDescription.text.toString().trim()
            val uris = selectedUris.filterNotNull()
            viewModel.uploadProduct(title, price, category, description, uris)
        }
    }

    private fun showImagePickerOptions() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(com.example.nexcart.R.layout.layout_image_picker_bottom_sheet, null)
        view.findViewById<View>(com.example.nexcart.R.id.llCamera).setOnClickListener {
            checkCameraPermissionAndOpen()
            dialog.dismiss()
        }
        view.findViewById<View>(com.example.nexcart.R.id.llGallery).setOnClickListener {
            galleryLauncher.launch("image/*")
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val file = File(requireContext().cacheDir, "temp_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        cameraLauncher.launch(cameraImageUri!!)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uploadState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.btnSubmit.isEnabled = false
                            binding.btnSubmit.text = "Uploading…"
                        }
                        is UiState.Success<*> -> {
                            binding.btnSubmit.isEnabled = true
                            binding.btnSubmit.text = "List Product"
                        }
                        is UiState.Error -> {
                            binding.btnSubmit.isEnabled = true
                            binding.btnSubmit.text = "List Product"
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        is UiState.Idle -> binding.btnSubmit.isEnabled = true
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventFlow.collect { event ->
                    when (event) {
                        is AddProductViewModel.AddProductEvent.ShowToast ->
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        is AddProductViewModel.AddProductEvent.ProductAdded -> {
                            Toast.makeText(requireContext(), "Product listed successfully!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}