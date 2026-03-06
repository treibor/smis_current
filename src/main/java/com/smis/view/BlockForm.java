package com.smis.view;

import com.smis.dbservice.Dbservice;
import com.smis.entity.Block;
import com.smis.util.ValidationUtil;
import com.smis.view.BlockForm.BlockFormEvent;
import com.smis.view.BlockForm.DeleteEvent;
import com.smis.view.BlockForm.SaveEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;

public class BlockForm extends FormLayout{
	Dbservice service;
	Binder<Block> binder=new BeanValidationBinder<>(Block.class);
	TextField blockName=new TextField("Block/MB Name");
	TextField blockDevelopmentOfficer=new TextField("Office Head");
	TextField blockLabel=new TextField("Block/MB Label");
	Button save= new Button("Save");
	Button delete= new Button("Delete");
	Checkbox inUse=new Checkbox("In Use");
	private Block block;
	boolean isAdmin;
	public BlockForm(Dbservice service) {
		this.service=service;
		binder.bindInstanceFields(this);
		isAdmin=service.isAdmin();
		
		blockName.setHelperText("Eg: Mawlai or Shillong Municipal Board");
		blockDevelopmentOfficer.setHelperText("Eg: Block Development Officer");
		blockLabel.setHelperText("Eg: Mawlai C&RD Block");
		ValidationUtil.applyTextOnly(blockDevelopmentOfficer);
		ValidationUtil.applyValidation(blockLabel);
		ValidationUtil.applyTextOnly(blockName);
		add(blockName, blockDevelopmentOfficer, blockLabel, inUse, createButtonsLayout());
	}
	
	private Component createButtonsLayout() {
		// TODO Auto-generated method stub
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		save.addClickShortcut(Key.ENTER);
		save.addClickListener(event-> validateandSave());
		delete.addClickListener(event-> fireEvent(new DeleteEvent(this, block)));
		//delete.setEnabled(isadmin);
		delete.setEnabled(isAdmin);
		return new HorizontalLayout(save, delete);
		
	}
	private void validateandSave() {
		try {
			binder.writeBean(block);
			block.setDistrict(service.getDistrict());
			fireEvent(new SaveEvent(this, block));
		} catch (ValidationException e) {
			//notification.show("Please Enter All Required Fields",3000,Position.TOP_CENTER);
			
		}catch (Exception e) {
			
		}
		
	}

	public void setBlock(Block block) {
		this.block=block;
		binder.readBean(block);
	}
	
	public static abstract class BlockFormEvent extends ComponentEvent<BlockForm> {
		private Block block;

		protected BlockFormEvent(BlockForm source, Block block) {
			super(source, false);
			this.block = block;
		}

		public Block getBlock() {
			return block;
		}
	}

	public static class SaveEvent extends BlockFormEvent {
		SaveEvent(BlockForm source, Block block) {
			super(source, block);
		}
	}

	public static class DeleteEvent extends BlockFormEvent {
		DeleteEvent(BlockForm source, Block block) {
			super(source, block);
		}

	}

	public static class CloseEvent extends BlockFormEvent {
		CloseEvent(BlockForm source) {
			super(source, null);
		}
	}

	public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
			ComponentEventListener<T> listener) {
		return getEventBus().addListener(eventType, listener);
	}
}
