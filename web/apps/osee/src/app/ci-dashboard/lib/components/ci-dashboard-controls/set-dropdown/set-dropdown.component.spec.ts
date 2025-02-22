/*********************************************************************
 * Copyright (c) 2023 Boeing
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

import { SetDropdownComponent } from './set-dropdown.component';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import {
	provideHttpClient,
	withInterceptorsFromDi,
} from '@angular/common/http';
import { CiSetRoutedUiService } from '../../../services/ci-set-routed-ui.service';
import { ciSetRoutedUiServiceMock } from '../../../testing/ci-set-routed-ui.servoce.mock';

describe('SetDropdownComponent', () => {
	let component: SetDropdownComponent;
	let fixture: ComponentFixture<SetDropdownComponent>;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [SetDropdownComponent, NoopAnimationsModule],
			providers: [
				{
					provide: CiSetRoutedUiService,
					useValue: ciSetRoutedUiServiceMock,
				},
				provideHttpClient(withInterceptorsFromDi()),
				provideHttpClientTesting(),
			],
		});
		fixture = TestBed.createComponent(SetDropdownComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
