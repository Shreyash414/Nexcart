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
import com.bumptech.glide.Glide
import com.example.nexcart.databinding.FragmentAddProductBinding
import com.example.nexcart.utils.UiState
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddProductViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            updateImagePreview()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selectedImageUri = cameraImageUri
            updateImagePreview()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryDropdown()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Electronics", "Fashion", "Home", "Beauty", "Books", "Toys")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.cardImage.setOnClickListener {
            showImagePickerOptions()
        }

        binding.btnSubmit.setOnClickListener {
            val title = binding.etProductName.text.toString().trim()
            val price = binding.etProductPrice.text.toString().trim()
            val category = binding.actvCategory.text.toString().trim()
            val description = binding.etProductDescription.text.toString().trim()

            viewModel.uploadProduct(title, price, category, description, selectedImageUri)
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
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val file = File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        cameraLauncher.launch(cameraImageUri!!)
    }

    private fun updateImagePreview() {
        binding.ivProduct.isVisible = true
        binding.layoutUpload.isVisible = false
        Glide.with(this).load(selectedImageUri).into(binding.ivProduct)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uploadState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.btnSubmit.isEnabled = false
                            binding.btnSubmit.text = "Uploading..."
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
                        is UiState.Idle -> {
                            binding.btnSubmit.isEnabled = true
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventFlow.collect { event ->
                    when (event) {
                        is AddProductViewModel.AddProductEvent.ShowToast -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
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