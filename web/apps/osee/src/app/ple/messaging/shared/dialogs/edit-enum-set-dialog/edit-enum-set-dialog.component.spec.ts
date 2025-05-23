/*********************************************************************
 * Copyright (c) 2021 Boeing
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Boeing - initial API and implementation
 **********************************************************************/
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { AsyncPipe, NgIf } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonHarness } from '@angular/material/button/testing';
import {
	MAT_DIALOG_DATA,
	MatDialogModule,
	MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { UserDataAccountService } from '@osee/auth';
import { userDataAccountServiceMock } from '@osee/auth/testing';
import {
	EnumerationSetService,
	MimPreferencesService,
	TypesService,
} from '@osee/messaging/shared/services';
import {
	enumerationSetServiceMock,
	MimPreferencesServiceMock,
	MockEditEnumSetFieldComponent,
	typesServiceMock,
} from '@osee/messaging/shared/testing';
import { ApplicabilityListService } from '@osee/shared/services';
import { applicabilityListServiceMock } from '@osee/shared/testing';
import { of } from 'rxjs';

import { PlatformTypeSentinel } from '@osee/messaging/shared/enumerations';
import { EditEnumSetDialogComponent } from './edit-enum-set-dialog.component';

describe('EditEnumSetDialogComponent', () => {
	let component: EditEnumSetDialogComponent;
	let fixture: ComponentFixture<EditEnumSetDialogComponent>;
	let loader: HarnessLoader;

	beforeEach(async () => {
		await TestBed.overrideComponent(EditEnumSetDialogComponent, {
			set: {
				imports: [
					NgIf,
					AsyncPipe,
					MatDialogModule,
					MatButtonModule,
					MockEditEnumSetFieldComponent,
				],
				providers: [
					{
						provide: MatDialogRef,
						useValue: {
							close() {
								return of();
							},
						},
					},
					{
						provide: MAT_DIALOG_DATA,
						useValue: {
							platformType: new PlatformTypeSentinel(),
							isOnEditablePage: true,
						},
					},
					{
						provide: EnumerationSetService,
						useValue: enumerationSetServiceMock,
					},
					{
						provide: ApplicabilityListService,
						useValue: applicabilityListServiceMock,
					},
					{
						provide: MimPreferencesService,
						useValue: MimPreferencesServiceMock,
					},
					{
						provide: UserDataAccountService,
						useValue: userDataAccountServiceMock,
					},
					{ provide: TypesService, useValue: typesServiceMock },
				],
			},
		})
			.configureTestingModule({
				declarations: [],
				imports: [
					MatDialogModule,
					MatIconModule,
					MatSelectModule,
					MatInputModule,
					MatFormFieldModule,
					FormsModule,
					MatTableModule,
					NoopAnimationsModule,
					EditEnumSetDialogComponent,
					MockEditEnumSetFieldComponent,
				],
				providers: [
					{
						provide: MatDialogRef,
						useValue: {
							close() {
								return of();
							},
						},
					},
					{
						provide: MAT_DIALOG_DATA,
						useValue: {
							platformType: new PlatformTypeSentinel(),
							isOnEditablePage: true,
						},
					},
					{
						provide: EnumerationSetService,
						useValue: enumerationSetServiceMock,
					},
					{
						provide: ApplicabilityListService,
						useValue: applicabilityListServiceMock,
					},
					{
						provide: MimPreferencesService,
						useValue: MimPreferencesServiceMock,
					},
					{
						provide: UserDataAccountService,
						useValue: userDataAccountServiceMock,
					},
					{ provide: TypesService, useValue: typesServiceMock },
				],
			})
			.compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(EditEnumSetDialogComponent);
		component = fixture.componentInstance;
		loader = TestbedHarnessEnvironment.loader(fixture);
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should close the dialog', async () => {
		const dialogRefClosure = spyOn(component.dialogRef, 'close').and.stub();
		const button = await loader.getHarness(
			MatButtonHarness.with({ text: 'Cancel' })
		);
		await button.click();
		expect(dialogRefClosure).toHaveBeenCalled();
	});
});
