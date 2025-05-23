/*********************************************************************
 * Copyright (c) 2025 Boeing
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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CiAdminConfigComponent } from './ci-admin-config.component';
import { CiAdminConfigService } from '../../../services/ci-admin-config.service';
import { ciAdminConfigServiceMock } from '../../../testing/ci-admin-config.service.mock';
import { DashboardHttpService } from '../../../services/dashboard-http.service';
import { dashboardHttpServiceMock } from '../../../services/dashboard-http.service.mock';
import { TransactionService } from '@osee/transactions/services';
import { transactionServiceMock } from '@osee/transactions/services/testing';

describe('CiAdminConfigComponent', () => {
	let component: CiAdminConfigComponent;
	let fixture: ComponentFixture<CiAdminConfigComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [CiAdminConfigComponent],
			providers: [
				{
					provide: CiAdminConfigService,
					useValue: ciAdminConfigServiceMock,
				},
				{
					provide: DashboardHttpService,
					useValue: dashboardHttpServiceMock,
				},
				{
					provide: TransactionService,
					useValue: transactionServiceMock,
				},
			],
		}).compileComponents();

		fixture = TestBed.createComponent(CiAdminConfigComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
